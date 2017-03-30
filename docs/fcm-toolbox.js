"use strict";

var DEFAULT_APP_SETTINGS = {fcm: {apiKey: 'AAAASwElybY:APA91bFaTT_zKLcLYqB0soW8PJmFFG7x1F3wiR0MGta9lLsU22uAVa0VD_3zzz-OremJKDEWEf52OD554byamcwAmZldgrQKfwAjjbhZz_5DYT-z1gcflUBFSWVQQ9lSE9KwDBNHULvfVKmQwxa7xNwuPHz-VfdTbw', ttl:60,priority: 'high'},frd: {databaseUrl: 'https://fir-cloudmessaging-4e2cd.firebaseio.com'}};

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
  return value && JSON.parse(value);
}

function getSettings(settings) {
  return localStorage.getObject('settings') || DEFAULT_APP_SETTINGS;
}

function setSettings(settings) {
  return localStorage.setObject('settings', settings);
}

function getDeviceTokens() {
  return localStorage.getObject('device_tokens');
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

function showLoading() {
  $('#btn-send').hide();
  $('#btn-loading').show();
}

function hideLoading() {
  $('#btn-loading').hide();
  $('#btn-send').show();
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

  ;
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
    let users = localStorage.getObject('fcm_users');
    for (let user in users) {
      let connections = users[user].connections;
      for (let connection in connections) {
        let data = connections[connection];
        if (data.displayName && data.token === currentToken) {
          isConnectedUser = true;
        }
      }
    }

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

      let tokens = getDeviceTokens() || [];
      for (let i = 0; i < tokens.length; i++) {
        if (tokens[i].value === currentToken) {
          aliasBtn.text(tokens[i].label);
          break;
        }
      }
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
}

function initFirebase() {
  localStorage.removeItem('fcm_users');
  let settings = getSettings();
  if (!settings.frd.databaseUrl) return;

  let config = {databaseURL: settings.frd.databaseUrl};
  firebase.initializeApp(config);
  let devices = firebase.database().ref('users');
  devices.on('value', function(snapshot) {
    let users = snapshot.val();
    localStorage.setObject('fcm_users', users);
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
  let tokens = getDeviceTokens();
  if (!tokens) {
    return;
  }

  for (let i = 0; i < tokens.length; i++) {
    let token = tokens[i];
    let trash = $('<i class="fa fa-trash">&nbsp;</i>');
    trash.click(function(event) {
      if (confirm("Delete device token?")) {
        removeDeviceToken(token);
        $('#form-device-token').change();
      }
      event.preventDefault();
    });
    list.append(
      $('<a href="#" class="list-group-item list-group-item-action"></a>')
      .attr('data-token', token.value)
      .text(token.label)
      .prepend(trash)
    );
  }
  bindDeviceTokens();
}

function bindDeviceTokens() {
  findDeviceTokenList().find("a.list-group-item").click(function(event) {
    findDeviceToken().val($(this).attr('data-token')).change();
    event.preventDefault();
  });
  findDeviceUserList().find("a.list-group-item").click(function(event) {
    findDeviceToken().val($(this).attr('data-token')).change();
    event.preventDefault();
  });
  updateDevicesAndUsersStatus();
}

function addDeviceToken(value, label) {
  if (value && label) {
    let tokens = getDeviceTokens() || [];
    let newToken = {
      value: value,
      label: label,
      timestamp: Date.now()
    };
    tokens.push(newToken);
    setDeviceTokens(tokens);
    displayDeviceTokens();
  }
}

function removeDeviceToken(token) {
  let tokens = getDeviceTokens() || [];
  for (let i = 0; i < tokens.length; i++) {
    if (tokens[i].timestamp === token.timestamp) {
      tokens.splice(i, 1);
      break;
    }
  }
  setDeviceTokens(tokens);
  displayDeviceTokens();
}

function displayConnectedDevices() {
  let users = localStorage.getObject('fcm_users');
  let list = findDeviceUserList();
  list.empty();
  if (!users) {
    return;
  }
  for (let user in users) {
    let connections = users[user].connections;
    if (connections) {
      for (let connection in connections) {
        let data = connections[connection];
        if (data.displayName && data.token) {
          let add = $('<i class="fa fa-plus">&nbsp;</i>');
          add.click(function(event) {
            addDeviceToken(data.token, data.displayName);
            event.preventDefault();
          });
          list.append(
            $('<a href="#" class="list-group-item list-group-item-action list-group-item-success"></a>')
            .attr('data-token', data.token)
            .text(data.displayName)
            .prepend(add)
          );
        }
      }
    }
  }
  bindDeviceTokens();
}

function updateDevicesAndUsersStatus() {
  let tokens = findDeviceTokenList();
  let devices = findDeviceUserList();
  let users = localStorage.getObject('fcm_users');
  // reset status
  tokens.find("a.list-group-item").removeClass("list-group-item-success");
  devices.find("a.list-group-item").show();
  if (!users) {
    return;
  }
  for (let user in users) {
    let connections = users[user].connections;
    if (connections) {
      for (let connection in connections) {
        let data = connections[connection];
        if (data.displayName && data.token) {
          let userListedAsToken = tokens.find("a.list-group-item[data-token='" + data.token + "']");
          let userListedAsDevice = devices.find("a.list-group-item[data-token='" + data.token + "']");
          if (userListedAsToken && userListedAsToken.length > 0) {
            userListedAsToken.addClass("list-group-item-success");
            userListedAsDevice.hide();
            continue;
          }
        }
      }
    }
  }
}

function triggerSendMessage() {
  if (showSettingsIfNeeded()) return;
  showLoading();
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
      hideLoading();
      let alert = $('<div class="alert alert-success alert-dismissible fade in" role="alert" data-alert-timeout="10000" style="display: none;"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><span data-placeholder></span></div>');
      alert.find("span[data-placeholder]").text(JSON.stringify(data));
      $("#alert-container").append(alert);
      alert.slideDown();
      alert.delay(10000).fadeOut(function() {
        alert.remove();
      });
    },
    error: function(data) {
      hideLoading();
      let alert = $('<div class="alert alert-danger alert-dismissible fade in" role="alert" data-alert-timeout="20000" style="display: none;"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><span data-placeholder></span></div>');
      alert.find("span[data-placeholder]").text(JSON.stringify(data));
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

  //let payload = {"to" : token, "data" : {"ping" : "pong"}, ttl: ttl};
  let payload = {
    "to": token,
    ttl: ttl,
    priority: priority
  };

  switch (active.attr('href')) {
    case "#send-ping": {
      payload.data = {
        ping: {}
      };
      return payload;
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
      return payload;
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
      return payload;
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
      return payload;
    }
    case "#send-raw": {
      payload.data = JSON.parse($("#send-raw-data").val());
      return payload;
    }
  }
}
