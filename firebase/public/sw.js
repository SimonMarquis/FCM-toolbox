"use strict";

const cacheName = "fcm-1.1.3";

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(cacheName).then(function(cache) {
      return cache
        .addAll(["./", "./index.html", "./fcm-toolbox.css", "./fcm-toolbox.js", "./favicon.png", "./192.png", "./512.png"])
        .then(() => self.skipWaiting());
    })
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("fetch", event => {
  event.respondWith(
    caches
      .open(cacheName)
      .then(cache => cache.match(event.request))
      .then(response => response || fetch(event.request))
  );
});
