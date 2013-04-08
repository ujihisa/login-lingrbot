(ns login-lingrbot.core
  (:require [clj-http.client :as client])
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
  (prn 'hmm (.getFile (clojure.java.io/resource "bot-verifier")))
  (if-let [bot-verifier (clojure.string/trim-newline
                          (.getFile (clojure.java.io/resource "bot-verifier")))]
    (read-command (clojure.string/split "sudo journalctl -u systemd-logind -f" #" ") [line]
      (when (re-find #"New session" line)
        (let [msg (clojure.string/trim-newline line)]
          (client/get (make-lingr-url "computer_science" msg bot-verifier)))))
    (.out *err* "give me bot-verifier")))

; vim: set lispwords+=read-command :
