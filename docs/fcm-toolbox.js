"use strict";

var DEFAULT_APP_SETTINGS = {
    fcm: {
        apiKey: "AAAASwElybY:APA91bFaTT_zKLcLYqB0soW8PJmFFG7x1F3wiR0MGta9lLsU22uAVa0VD_3zzz-OremJKDEWEf52OD554byamcwAmZldgrQKfwAjjbhZz_5DYT-z1gcflUBFSWVQQ9lSE9KwDBNHULvfVKmQwxa7xNwuPHz-VfdTbw",
        ttl: 60,
        priority: "high"
    },
    frd: {
        databaseUrl: "https://fir-cloudmessaging-4e2cd.firebaseio.com"
    },
    hide: false
};

var PWA;

$(function() {
    initServiceWorker();
    initPWA();
    bind();
    initSettings();
    displayDeviceTokens();
    showDefaultDeviceToken();
    initFirebase();
});


function initServiceWorker() {
    if ("serviceWorker" in navigator) {
        navigator.serviceWorker
            .register("sw.js", { scope: "./" })
            .then(function(registration) {
                console.log("Service Worker Registered");
            })
            .catch(function(err) {
                console.log("Service Worker Registration Failed: ", err);
            });
        navigator.serviceWorker.ready.then(function(registration) {
            console.log("Service Worker Ready");
        });
    }
}

function initPWA() {
    window.addEventListener("beforeinstallprompt", function(event) {
        event.preventDefault();
        PWA = event;
        document.getElementById("pwa").removeAttribute("hidden");
    });
    window.addEventListener("appinstalled", function(event) {
        hideSettings();
    });
    document.getElementById("pwa").addEventListener("click", function(event) {
        PWA.prompt();
        PWA.userChoice.then(function(result) {
            console.log("PWA result:", result);
            document.getElementById("pwa").setAttribute("hidden", "true");
        });
        event.preventDefault();
        event.stopPropagation();
    }, false);
}

Storage.prototype.setObject = function(key, value) {
    this.setItem(key, JSON.stringify(value));
};

Storage.prototype.getObject = function(key) {
    var value = this.getItem(key);
    try {
        return value && JSON.parse(value);
    } catch (e) {
        return undefined;
    }
};

function getSettings(settings) {
    return localStorage.getObject("settings") || DEFAULT_APP_SETTINGS;
}

function setSettings(settings) {
    return localStorage.setObject("settings", settings);
}

function getDeviceTokens() {
    return localStorage.getObject("device_tokens") || [];
}

function setDeviceTokens(tokens) {
    return localStorage.setObject("device_tokens", tokens);
}

function getLastDeviceToken() {
    return localStorage.getObject("last_device_token");
}

function setLastDeviceToken(token) {
    return localStorage.setObject("last_device_token", token);
}

function getFcmUsers() {
    return localStorage.getObject("fcm_users") || [];
}

function setFcmUsers(users) {
    localStorage.setObject("fcm_users", users);
}

function findDeviceToken() {
    return $("#form-device-token");
}

function findDeviceTokenList() {
    return $("#list-device-tokens");
}

function findDeviceUserList() {
    return $("#list-device-users");
}

function resetAndReload() {
    if (confirm("Reset data?")) {
        localStorage.clear();
        location.reload();
    }
}

function toggleNotificationVisibility() {
    var settings = getSettings();
    settings.hide = !settings.hide;
    setSettings(settings);
    renderNotificationVisibility();
}

function renderNotificationVisibility() {
    var hide = Boolean(getSettings().hide);
    $("#btn-visibility-icon")
        .text(hide ? "notifications_off" : "notifications")
        .parent()
        .addClass(hide ? "btn-outline-primary" : "btn-primary")
        .removeClass(hide ? "btn-primary" : "btn-outline-primary");
}

function showSettingsIfNeeded() {
    if (!getSettings().fcm.apiKey) {
        showSettings();
        return true;
    }
    return false;
}

function showSettings() {
    $("#settings-modal").modal("show");
}

function hideSettings() {
    $("#settings-modal").modal("hide");
}

function bind() {
    $("body").keydown(function(e) {
        if (e.ctrlKey && e.keyCode === 13) {
            triggerSendMessage();
        }
    });
    $("#btn-reset").click(function(event) {
        resetAndReload();
        $(this).blur();
    });
    $("#btn-visibility").click(function() {
        $(this).blur();
        toggleNotificationVisibility();
    });
    $("#btn-send").click(function() {
        $(this).blur();
        triggerSendMessage();
    });
    $("#btn-add-device-token").click(function() {
        $(this).blur();
        var name = prompt("Enter a name for this device token");
        if (name != null && name) {
            addDeviceToken(findDeviceToken().val(), name);
            $("#form-device-token").change();
        }
    });
    $("#send-raw-data").bind("input propertychange change", function() {
        var element = $(this);
        try {
            $.parseJSON(element.val());
            element.removeClass("is-invalid").addClass("is-valid");
        } catch (err) {
            element.removeClass("is-valid").addClass("is-invalid");
        }
    });
    $("#send-raw-data").focusout(function() {
            var element = $(this);
            try {
                element.val(JSON.stringify(JSON.parse(element.val()), undefined, 4));
                element.attr("rows", Math.max(3, (element.val().match(/\n/g) || []).length + 1));
            } catch (err) {}
        })
        .val(JSON.stringify({
            key: "value"
        }, undefined, 4))
        .change();
    $("#form-device-token").bind("input change", function() {
        var element = $(this);
        var currentToken = element.val();
        setLastDeviceToken(currentToken);

        var addBtn = $("#btn-add-device-token");
        var sendBtn = $("#btn-send");
        var aliasBtn = $("#btn-device-token-alias");
        var dropdownBtn = $("#btn-device-dropdown");
        aliasBtn.empty();

        var isConnectedUser = false;
        $.each(getFcmUsers(), function(index, user) {
            if (user.value === currentToken) {
                isConnectedUser = true;
            }
        });

        $.each([element, aliasBtn], function(i, l) {
            if (isConnectedUser) {
                l.addClass("list-group-item-success");
            } else {
                l.removeClass("list-group-item-success");
            }
        });

        if (currentToken) {
            element.removeClass("is-invalid");
            addBtn.prop("disabled", false);
            sendBtn.prop("disabled", false);

            $.each(getDeviceTokens(), function(index, token) {
                if (token.value === currentToken) {
                    aliasBtn.text(token.label);
                    return false;
                }
            });
        } else {
            element.addClass("is-invalid");
            addBtn.prop("disabled", true);
            sendBtn.prop("disabled", true);
        }

        if (aliasBtn.text()) {
            addBtn.hide();
            aliasBtn.show();
        } else {
            aliasBtn.hide();
            addBtn.show();
        }
    });
}

function initSettings() {
    var settings = getSettings();
    var fcmApiKey = $("#settings-fcm-api-key");
    var fcmTtl = $("#settings-fcm-ttl");
    var fcmPriority = $("#settings-fcm-priority");
    var frdDatabaseUrl = $("#settings-frd-database-url");

    fcmApiKey.val(settings.fcm.apiKey);
    fcmApiKey.bind("input", function() {
        fcmApiKey.parent().change();
        settings.fcm.apiKey = $(this).val();
        setSettings(settings);
    });
    fcmApiKey.parent().bind("change", function() {
        if (fcmApiKey.val()) {
            fcmApiKey.removeClass("is-invalid");
        } else {
            fcmApiKey.addClass("is-invalid");
        }
    }).change();

    fcmTtl.val(settings.fcm.ttl);
    fcmTtl.bind("input", function() {
        settings.fcm.ttl = $(this).val();
        setSettings(settings);
    });
    fcmPriority.val(settings.fcm.priority);
    fcmPriority.bind("input", function() {
        settings.fcm.priority = $(this).val();
        setSettings(settings);
    });
    frdDatabaseUrl.val(settings.frd.databaseUrl);
    frdDatabaseUrl.bind("input", function() {
        settings.frd.databaseUrl = $(this).val();
        setSettings(settings);
    });

    showSettingsIfNeeded();
    renderNotificationVisibility();
}

function initFirebase() {
    setFcmUsers(undefined);
    var settings = getSettings();
    if (!settings.frd.databaseUrl) {
        return;
    }

    var config = {
        databaseURL: settings.frd.databaseUrl
    };
    firebase.initializeApp(config);
    var devices = firebase.database().ref("devices");
    devices.on("value", function(snapshot) {
        var users = snapshot.val();
        var items = [];
        $.each(snapshot.val() || [], function(key, user) {
            if (user.token) {
                items.push({
                    timestamp: user.timestamp,
                    label: user.name,
                    value: user.token
                });
            }
        });
        items.sort(function(a, b) {
            return a.timestamp - b.timestamp;
        });
        setFcmUsers(items);
        displayConnectedDevices();
        findDeviceToken().change();
    });
}

function showDefaultDeviceToken() {
    findDeviceToken().val(getLastDeviceToken() || "").change();
}

function displayDeviceTokens() {
    var list = findDeviceTokenList();
    list.empty();
    $.each(getDeviceTokens(), function(index, token) {
        var trash = $("<i class='material-icons align-middle'>clear</i>");
        trash.click(function(event) {
            removeDeviceToken(token);
            $("#form-device-token").change();
            event.preventDefault();
            event.stopPropagation();
        });
        list.append(
            $("<a href='' class='dropdown-item dropdown-item-action list-group-item-light'></a>")
            .attr("data-token", token.value)
            .text(token.label)
            .prepend("&nbsp;&nbsp;")
            .prepend(trash)
        );
    });
    bindDeviceTokens();
    updateEmptyDeviceListHeader();
}

function bindDeviceTokens() {
    findDeviceTokenList().find("a.dropdown-item-action").click(function(event) {
        findDeviceToken().val($(this).attr("data-token")).change();
        event.preventDefault();
    });
    findDeviceUserList().find("a.dropdown-item-action").click(function(event) {
        findDeviceToken().val($(this).attr("data-token")).change();
        event.preventDefault();
    });
    updateDevicesAndUsersStatus();
}

function addDeviceToken(value, label) {
    if (value && label) {
        var tokens = getDeviceTokens();
        var newToken = {
            value: value,
            label: label,
            timestamp: Date.now()
        };
        tokens.push(newToken);
        tokens.sort(function(a, b) {
            return a.timestamp - b.timestamp;
        });
        setDeviceTokens(tokens);
        displayDeviceTokens();
    }
}

function removeDeviceToken(token) {
    var tokens = getDeviceTokens();
    var grep = $.grep(tokens, function(t, index) {
        return t.value !== token.value;
    });
    setDeviceTokens(grep);
    displayDeviceTokens();
}

function displayConnectedDevices() {
    var users = getFcmUsers();
    var list = findDeviceUserList();
    list.empty();
    users.forEach(function(user, index) {
        var add = $("<i class='material-icons align-middle'>add</i>");
        add.click(function(event) {
            addDeviceToken(user.value, user.label);
            $("#form-device-token").change();
            event.preventDefault();
            event.stopPropagation();
        });
        list.append(
            $("<a href='' class='dropdown-item dropdown-item-action list-group-item-light text-success'></a>")
            .attr("data-token", user.value)
            .text(user.label)
            .prepend("&nbsp;&nbsp;")
            .prepend(add)
        );
    });
    bindDeviceTokens();
    updateEmptyDeviceListHeader();
}

function updateDevicesAndUsersStatus() {
    var tokens = findDeviceTokenList();
    var devices = findDeviceUserList();
    // reset status
    tokens.find("a.dropdown-item").removeClass("text-success");
    devices.find("a.dropdown-item").show();

    $.each(getFcmUsers(), function(index, user) {
        var userListedAsToken = tokens.find("a.dropdown-item[data-token='" + user.value + "']");
        var userListedAsDevice = devices.find("a.dropdown-item[data-token='" + user.value + "']");
        if (userListedAsToken && userListedAsToken.length > 0) {
            userListedAsToken.addClass("text-success");
            userListedAsDevice.hide();
        }
    });
}

function updateEmptyDeviceListHeader() {
    if (findDeviceTokenList().children().length > 0 || findDeviceUserList().children().length > 0) {
        $("#list-device-empty").hide();
    } else {
        $("#list-device-empty").show();
    }
}

function triggerSendMessage() {
    if (showSettingsIfNeeded()) {
        return;
    }
    var settings = getSettings();
    var key = settings.fcm.apiKey;
    var payload = buildPayload();
    $.ajax({
        url: "https://fcm.googleapis.com/fcm/send",
        type: "post",
        beforeSend: function(request) {
            request.setRequestHeader("Authorization", "key=" + key);
            request.setRequestHeader("Content-Type", "application/json");
        },
        data: JSON.stringify(payload),
        dataType: "json",
        success: function(data) {
            var alert = $("<div class='alert alert-success alert-dismissible fade show' role='alert' data-alert-timeout='10000' style='display: none;'><button type='button' class='close' data-dismiss='alert' aria-label='Close'><span aria-hidden='true'>&times;</span></button><pre data-request></pre><hr/><pre data-response></pre></div>");
            alert.find("pre[data-request]").text(JSON.stringify(payload, null, 2));
            alert.find("pre[data-response]").text(JSON.stringify(data, null, 2));
            $("#alert-container").prepend(alert);
            alert.slideDown();
            alert.delay(10000).fadeOut(function() {
                alert.remove();
            });
        },
        error: function(data) {
            var alert = $("<div class='alert alert-danger alert-dismissible fade show' role='alert' data-alert-timeout='20000' style='display: none;'><button type='button' class='close' data-dismiss='alert' aria-label='Close'><span aria-hidden='true'>&times;</span></button><pre data-request></pre><hr/><pre data-response></pre></div>");
            alert.find("pre[data-request]").text(JSON.stringify(payload, null, 2));
            alert.find("pre[data-response]").text(JSON.stringify(data, null, 2));
            $("#alert-container").prepend(alert);
            alert.slideDown();
            alert.delay(20000).fadeOut(function() {
                alert.remove();
            });
        }
    });
}

function buildPayload() {
    var active = $("#payload-type").find(".active[role=tab]");
    if (!active) {
        return;
    }
    var settings = getSettings();

    var token = findDeviceToken().val();
    var ttl = parseInt(settings.fcm.ttl);
    var priority = settings.fcm.priority;

    var payload = {
        to: token,
        time_to_live: ttl,
        priority: priority
    };

    switch (active.attr("href")) {
        case "#send-ping":
            payload.data = {
                ping: {}
            };
            break;
        case "#send-text":
            payload.data = {
                text: {
                    title: $("#send-text-title").val(),
                    message: $("#send-text-message").val(),
                    clipboard: $("#send-text-clipboard").is(":checked")
                }
            };
            break;
        case "#send-link":
            payload.data = {
                link: {
                    title: $("#send-link-title").val(),
                    url: $("#send-link-url").val(),
                    open: $("#send-link-url-open").is(":checked")
                }
            };
            break;
        case "#send-app":
            payload.data = {
                app: {
                    title: $("#send-app-title").val(),
                    package: $("#send-app-package").val()
                }
            };
            break;
        case "#send-raw":
            payload.data = JSON.parse($("#send-raw-data").val());
            break;
        default:
            return null;
    }
    if (settings.hide) {
        payload.data.hide = true;
    }
    return payload;
}