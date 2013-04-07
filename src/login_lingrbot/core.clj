(ns login-lingrbot.core
  (:require [clj-http.client :as client])
  (:import [java.net URLEncoder]))

(defn make-lingr-url [room text bot-verifier]
  (format
    "http://lingr.com/api/room/say?room=%s&bot=login&text=%s&bot_verifier=%s"
    room text bot-verifier))

(defn -main []
  (if-let [bot-verifier (clojure.string/trim-newline (slurp "src/resources/bot-verifier"))]
    (let [msg (URLEncoder/encode "てすと from clojure")
          lingr-url (make-lingr-url "computer_science" msg bot-verifier)]
      (client/get lingr-url))
    (.out *err* "give me bot-verifier")))
