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
    (read-command (clojure.string/split "sudo journalctl -n 0 -u systemd-logind -o json -f" #" ") [line]
      (when-let [json (try (json/read-str line)
                        (catch Exception e nil))]
        (when-let [user (json "USER_ID")]
          (let [host (json "_HOSTNAME")
                session-id (json "SESSION_ID")
                code-function (json "CODE_FUNCTION")
                login-template (or (let [fname (format "/home/%s/.login-lingrbotrc" user)]
                                     (when-let [rc (try (slurp fname)
                                                     (catch Exception e nil))]
                                       (:login (read-string rc))))
                                   "$USER_ID, welcome to $_HOSTNAME! ($SESSION_ID)")
                msg (case code-function
                      "session_start"
                      (let [x1 (clojure.string/replace login-template #"\$USER_ID" user)
                            x2 (clojure.string/replace x1 #"\$_HOSTNAME!" host)
                            x3 (clojure.string/replace x2 #"\$SESSION_ID" session-id)]
                        x3)
                      "session_stop"
                      (format "goodbye, %s from %s.. (%s)" user host session-id)
                      nil)]
            (when msg
              (client/get (make-lingr-url "computer_science" msg bot-verifier)))))))
    (.out *err* "give me bot-verifier")))

; vim: set lispwords+=read-command :
