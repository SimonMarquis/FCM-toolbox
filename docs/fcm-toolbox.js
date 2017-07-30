"use strict";

var DEFAULT_APP_SETTINGS = {fcm: {apiKey: 'AAAASwElybY:APA91bFaTT_zKLcLYqB0soW8PJmFFG7x1F3wiR0MGta9lLsU22uAVa0VD_3zzz-OremJKDEWEf52OD554byamcwAmZldgrQKfwAjjbhZz_5DYT-z1gcflUBFSWVQQ9lSE9KwDBNHULvfVKmQwxa7xNwuPHz-VfdTbw', ttl:60,priority: 'high'},frd: {databaseUrl: 'https://fir-cloudmessaging-4e2cd.firebaseio.com'}, hide: false};

$(function() {
  bind();
  initSettings();
  displayDeviceTokens();
  showDefaultDeviceToken();
  initFirebase();
});

Storage.prototype.setObject = function(key, value) {
  this.setItem(key, JSON.stringify(value));
}

Storage.prototype.getObject = function(key) {
  let value = this.getItem(key);
  try {
    return value && JSON.parse(value);
  } catch (e) {
    return undefined;
  }
}

function getSettings(settings) {
  return localStorage.getObject('settings') || DEFAULT_APP_SETTINGS;
}

function setSettings(settings) {
  return localStorage.setObject('settings', settings);
}

function getDeviceTokens() {
  return localStorage.getObject('device_tokens') || [];
}

function setDeviceTokens(tokens) {
  return localStorage.setObject('device_tokens', tokens);
}

function getLastDeviceToken() {
  return localStorage.getObject('last_device_token');
}

function setLastDeviceToken(token) {
  return localStorage.setObject('last_device_token', token);
}

function getFcmUsers() {
  return localStorage.getObject('fcm_users') || [];
}

function setFcmUsers(users) {
  localStorage.setObject('fcm_users', users)
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
  let settings = getSettings();
  settings.hide = !settings.hide;
  setSettings(settings);
  renderNotificationVisibility();
}

function renderNotificationVisibility() {
  let hide = Boolean(getSettings().hide);
  $('#btn-visibility-icon')
    .toggleClass('fa-eye', !hide)
    .toggleClass('fa-eye-slash', hide);
}

function showSettingsIfNeeded() {
  if (!getSettings().fcm.apiKey) {
    showSettings();
    return true;
  }
  return false;
}

function showSettings() {
  $('#settings-modal').modal('show');
}

function bind() {
  $('body').keydown(function(e) {
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
      $('#form-device-token').change();
    }
  });
  $('#send-raw-data').bind('input propertychange change', function(){
    let element = $(this);
    try {
      $.parseJSON(element.val());
      element.parent().removeClass('has-danger').addClass('has-success');
    }
    catch (err) {
      element.parent().removeClass('has-success').addClass('has-danger');
    }
  });
  $('#send-raw-data').focusout(function () {
    let element = $(this);
    try {
      element.val(JSON.stringify(JSON.parse(element.val()), undefined, 4));
      element.attr('rows', Math.max(4, (element.val().match(/\n/g)||[]).length + 1));
    } catch (err) {}
  })
  .val(JSON.stringify({key: 'value'}, undefined, 4))
  .change();
  $('#form-device-token').bind('input change', function(){
    let element = $(this);
    let currentToken = element.val();
    setLastDeviceToken(currentToken);

    let addBtn = $('#btn-add-device-token');
    let sendBtn = $('#btn-send');
    let aliasBtn = $('#btn-device-token-alias');
    let dropdownBtn = $('#btn-device-dropdown');
    aliasBtn.empty();

    let isConnectedUser = false;
    $.each(getFcmUsers(), function (index, user) {
      if (user.value === currentToken) {
        isConnectedUser = true;
      }
    });

    $.each( [ element, aliasBtn, addBtn, dropdownBtn ], function( i, l ){
      if (isConnectedUser) {
        l.addClass('list-group-item-success');
      } else {
        l.removeClass('list-group-item-success');
      }
    });

    if (currentToken) {
      element.parent().parent().parent().removeClass("has-danger");
      addBtn.prop('disabled', false);
      sendBtn.prop('disabled', false);

      $.each(getDeviceTokens(), function (index, token) {
        if (token.value === currentToken) {
          aliasBtn.text(token.label);
          return false;
        }
      });
    } else {
      element.parent().parent().parent().addClass("has-danger");
      addBtn.prop('disabled', true);
      sendBtn.prop('disabled', true);
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
  let settings = getSettings();
  let fcmApiKey = $('#settings-fcm-api-key');
  let fcmTtl = $('#settings-fcm-ttl');
  let fcmPriority = $('#settings-fcm-priority');
  let frdDatabaseUrl = $('#settings-frd-database-url');

  fcmApiKey.val(settings.fcm.apiKey);
  fcmApiKey.bind('input', function(){
    fcmApiKey.parent().change();
    settings.fcm.apiKey = $(this).val();
    setSettings(settings);
  });
  fcmApiKey.parent().bind('change', function(){
    if (fcmApiKey.val()) {
      $(this).removeClass("has-danger");
    } else {
      $(this).addClass("has-danger");
    }
  }).change();

  fcmTtl.val(settings.fcm.ttl);
  fcmTtl.bind('input', function(){
    settings.fcm.ttl = $(this).val();
    setSettings(settings);
  });
  fcmPriority.val(settings.fcm.priority);
  fcmPriority.bind('input', function(){
    settings.fcm.priority = $(this).val();
    setSettings(settings);
  });
  frdDatabaseUrl.val(settings.frd.databaseUrl);
  frdDatabaseUrl.bind('input', function(){
    settings.frd.databaseUrl = $(this).val();
    setSettings(settings);
  });

  showSettingsIfNeeded();
  renderNotificationVisibility();
}

function initFirebase() {
  setFcmUsers(undefined);
  let settings = getSettings();
  if (!settings.frd.databaseUrl) return;

  let config = {databaseURL: settings.frd.databaseUrl};
  firebase.initializeApp(config);
  let devices = firebase.database().ref('devices');
  devices.on('value', function(snapshot) {
    let users = snapshot.val();
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
    items.sort(function (a, b) {
      return a.timestamp - b.timestamp;
    });
    setFcmUsers(items);
    displayConnectedDevices();
    findDeviceToken().change();
  });
}

function showDefaultDeviceToken() {
  findDeviceToken().val(getLastDeviceToken() || '').change();
}

function displayDeviceTokens() {
  let list = findDeviceTokenList();
  list.empty();
  $.each(getDeviceTokens(), function (index, token) {
    let trash = $('<i class="fa fa-trash">&nbsp;&nbsp;</i>');
    trash.click(function(event) {
      removeDeviceToken(token);
      $('#form-device-token').change();
      event.preventDefault();
      event.stopPropagation();
    });
    list.append(
      $('<a href="#" class="dropdown-item dropdown-item-action"></a>')
      .attr('data-token', token.value)
      .text(token.label)
      .prepend(trash)
      );
  });
  bindDeviceTokens();
  updateEmptyDeviceListHeader();
}

function bindDeviceTokens() {
  findDeviceTokenList().find("a.dropdown-item-action").click(function(event) {
    findDeviceToken().val($(this).attr('data-token')).change();
    event.preventDefault();
  });
  findDeviceUserList().find("a.dropdown-item-action").click(function(event) {
    findDeviceToken().val($(this).attr('data-token')).change();
    event.preventDefault();
  });
  updateDevicesAndUsersStatus();
}

function addDeviceToken(value, label) {
  if (value && label) {
    let tokens = getDeviceTokens();
    let newToken = {
      value: value,
      label: label,
      timestamp: Date.now()
    };
    tokens.push(newToken);
    tokens.sort(function (a, b) {
      return a.timestamp - b.timestamp;
    });
    setDeviceTokens(tokens);
    displayDeviceTokens();
  }
}

function removeDeviceToken(token) {
  let tokens = getDeviceTokens();
  let grep = $.grep(tokens, function (t, index) {
    return t.value !== token.value;
  });
  setDeviceTokens(grep);
  displayDeviceTokens();
}

function displayConnectedDevices() {
  let users = getFcmUsers();
  let list = findDeviceUserList();
  list.empty();
  users.forEach(function (user, index) {
    let add = $('<i class="fa fa-plus">&nbsp;&nbsp;</i>');
    add.click(function(event) {
      addDeviceToken(user.value, user.label);
      $('#form-device-token').change();
      event.preventDefault();
      event.stopPropagation();
    });
    list.append(
      $('<a href="#" class="dropdown-item dropdown-item-action text-success"></a>')
      .attr('data-token', user.value)
      .text(user.label)
      .prepend(add)
      );
  });
  bindDeviceTokens();
  updateEmptyDeviceListHeader();
}

function updateDevicesAndUsersStatus() {
  let tokens = findDeviceTokenList();
  let devices = findDeviceUserList();
  // reset status
  tokens.find("a.dropdown-item").removeClass("text-success");
  devices.find("a.dropdown-item").show();

  $.each(getFcmUsers(), function (index, user) {
    let userListedAsToken = tokens.find("a.dropdown-item[data-token='" + user.value + "']");
    let userListedAsDevice = devices.find("a.dropdown-item[data-token='" + user.value + "']");
    if (userListedAsToken && userListedAsToken.length > 0) {
      userListedAsToken.addClass("text-success");
      userListedAsDevice.hide();
    }
  });
}

function updateEmptyDeviceListHeader() {
  if (findDeviceTokenList().children().length > 0 || findDeviceUserList().children().length > 0) {
    $('#list-device-empty').hide();
  } else {
    $('#list-device-empty').show();
  }
}

function triggerSendMessage() {
  if (showSettingsIfNeeded()) return;
  let settings = getSettings();
  let key = settings.fcm.apiKey;
  let payload = buildPayload();
  $.ajax({
    url: 'https://fcm.googleapis.com/fcm/send',
    type: 'post',
    beforeSend: function(request) {
      request.setRequestHeader("Authorization", 'key=' + key);
      request.setRequestHeader("Content-Type", 'application/json');
    },
    data: JSON.stringify(payload),
    dataType: 'json',
    success: function(data) {
      let alert = $('<div class="alert alert-success alert-dismissible fade in" role="alert" data-alert-timeout="10000" style="display: none;"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><pre data-placeholder></pre></div>');
      alert.find("pre[data-placeholder]").text(JSON.stringify(data, null, 2));
      $("#alert-container").append(alert);
      alert.slideDown();
      alert.delay(10000).fadeOut(function() {
        alert.remove();
      });
    },
    error: function(data) {
      let alert = $('<div class="alert alert-danger alert-dismissible fade in" role="alert" data-alert-timeout="20000" style="display: none;"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><pre data-placeholder></pre></div>');
      alert.find("pre[data-placeholder]").text(JSON.stringify(data, null, 2));
      $("#alert-container").append(alert);
      alert.slideDown();
      alert.delay(20000).fadeOut(function() {
        alert.remove();
      });
    }
  });
}

function buildPayload() {
  let active = $("#payload-type").find(".active[role=tab]");
  if (!active) {
    return;
  }
  let settings = getSettings();

  let token = findDeviceToken().val();
  let ttl = parseInt(settings.fcm.ttl);
  let priority = settings.fcm.priority;

  let payload = {
    to: token,
    time_to_live: ttl,
    priority: priority
  };

  switch (active.attr('href')) {
    case "#send-ping": {
      payload.data = {
        ping: {}
      };
      break;
    }
    case "#send-text": {
      let title = $("#send-text-title").val();
      let message = $("#send-text-message").val();
      let clipboard = $("#send-text-clipboard").is(':checked');
      payload.data = {
        text: {
          title: title,
          message: message,
          clipboard: clipboard
        }
      };
      break;
    }
    case "#send-link": {
      let title = $("#send-link-title").val();
      let url = $("#send-link-url").val();
      let open = $("#send-link-url-open").is(':checked');
      payload.data = {
        link: {
          title: title,
          url: url,
          open: open
        }
      };
      break;
    }
    case "#send-app": {
      let title = $("#send-app-title").val();
      let packageName = $("#send-app-package").val();
      payload.data = {
        app: {
          title: title,
          package: packageName
        }
      };
      break;
    }
    case "#send-raw": {
      payload.data = JSON.parse($("#send-raw-data").val());
      break;
    }
    default:
      return null;
  }
  if (settings.hide) {
    payload.data.hide = true;
  }
  return payload;
}
