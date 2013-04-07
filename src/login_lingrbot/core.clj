(ns login-lingrbot.core
  (:require [clj-http.client :as client]))

(defn lingr-url [room text bot-verifier]
  (format
    "http://lingr.com/api/room/say?room=%s&bot=login&text=%s&bot_verifier=%s"
    room text bot-verifier))

(defn -main []
  (prn (slurp "src/resources/lingr-post-url")))
