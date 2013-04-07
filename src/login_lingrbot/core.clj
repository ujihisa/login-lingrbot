(ns login-lingrbot.core
  (:require [clj-http.client :as client]))

(defn lingr-url [room text bot-verifier]
  (format
    "http://lingr.com/api/room/say?room=%s&bot=login&text=%s&bot_verifier=%s"
    room text bot-verifier))

(defn -main []
  (if-let [bot-verifier (slurp "src/resources/bot-verifier")]
    (prn 'bot-verifier bot-verifier)
    (.out *err* "give me bot-verifier")))
