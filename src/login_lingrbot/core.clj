(ns login-lingrbot.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:import [java.net URLEncoder])
  (:gen-class))

(defn read-command* [cmd f]
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd))]
    (with-open [stdout (.getInputStream proc)]
      (loop [acc []]
        (let [c (.read stdout)
              acc2 (conj acc c)]
          (condp = c
            10 (do
                 (f (apply str (map char acc2)))
                 (recur []))
            -1 (when-not (empty? acc)
                 (f (apply str (map char acc))))
            (recur acc2)))))))

(defmacro read-command [cmd args & body]
  `(read-command* ~cmd (fn ~args ~@body)))

(defn make-lingr-url [room text bot-verifier]
  (format
    "http://lingr.com/api/room/say?room=%s&bot=login&text=%s&bot_verifier=%s"
    room (URLEncoder/encode text) bot-verifier))

(defn -main []
  (if-let [bot-verifier (clojure.string/trim-newline
                          (slurp (clojure.java.io/resource "bot-verifier")))]
    (read-command (clojure.string/split "sudo journalctl -u systemd-logind -o json -f" #" ") [line]
      (when-let [json (json/read-str line)]
        (when-let [user (json "USER_ID")]
          (let [host (json "_HOSTNAME")
                session-id (json "SESSION_ID")
                msg (format "%s logged in to %s (%s)" user host session-id)]
            (client/get (make-lingr-url "computer_science" msg bot-verifier))))))
    (.out *err* "give me bot-verifier")))

; vim: set lispwords+=read-command :
