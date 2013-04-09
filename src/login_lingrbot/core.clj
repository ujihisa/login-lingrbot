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

(defn or-nil* [f]
  (try (f)
    (catch Exception e nil)))

(defmacro or-nil [body]
  `(or-nil* (fn [] ~@body)))

(defn -main [& args]
  (if-let [path-bot-verifier (first args)]
    (if-let [bot-verifier (clojure.string/trim-newline
                            (slurp #_(clojure.java.io/resource "bot-verifier")
                                   path-bot-verifier))]
      (read-command (clojure.string/split "sudo journalctl -n 0 -u systemd-logind -o json -f" #" ") [line]
        (when-let [json (or-nil (json/read-str line))]
          (when-let [user (json "USER_ID")]
            (let [code-function (json "CODE_FUNCTION")
                  login-template (or (let [fname (format "/home/%s/.login-lingrbotrc" user)]
                                       (when-let [rc (or-nil (slurp fname))]
                                         (rand-nth (:login (read-string rc)))))
                                     "$USER_ID, welcome to $_HOSTNAME! ($SESSION_ID)")
                  logout-template (or (let [fname (format "/home/%s/.login-lingrbotrc" user)]
                                        (when-let [rc (or-nil (slurp fname))]
                                          (rand-nth (:logout (read-string rc)))))
                                      "goodbye, $USER_ID from $_HOSTNAME!.. ($SESSION_ID)")
                  msg (case code-function
                        "session_start"
                        (reduce (fn [memo [k v]] (.replace memo (str "$" k) v)) login-template json)
                        "session_stop"
                        (reduce (fn [memo [k v]] (.replace memo (str "$" k) v)) logout-template json)
                        nil)]
              (when msg
                (client/get (make-lingr-url "computer_science" msg bot-verifier)))))))
      (.out *err* "give me bot-verifier"))
    (.out *err* "give me path of bot-verifier in command line argument.")))

; vim: set lispwords+=read-command :
