(ns ring-param-obfuscator.core
  (:refer-clojure :exclude [read read-string])
  (:use [clojure.tools.reader.edn :only [read read-string]]
        [clojure.set]
        [ring.middleware.params]
        [ring.util.codec])
  (:import (javax.crypto Cipher KeyGenerator SecretKey)
           (javax.crypto.spec SecretKeySpec)
           (java.security SecureRandom)
           (java.net URLEncoder)))
        
(defn- bytes [string]
  (.getBytes string "UTF-8"))

(defn- get-raw-key [seed]
  (let [keygen (KeyGenerator/getInstance "AES")
        sr (SecureRandom/getInstance "SHA1PRNG")]
    (.setSeed sr (bytes seed))
    (.init keygen 128 sr)
    (.. keygen generateKey getEncoded)))

(defn- get-cipher [mode seed]
  (let [key-spec (SecretKeySpec. (get-raw-key seed) "AES")
        cipher (Cipher/getInstance "AES")]
    (.init cipher mode key-spec)
    cipher))

(defn encrypt
  "Encrypt a string with AES using provided key"
  [text key]
  (let [bytes (bytes text)
        cipher (get-cipher Cipher/ENCRYPT_MODE key)]
    (base64-encode (.doFinal cipher bytes))))

(defn decrypt 
  "Decrypt a string with AES using provided key"
  [text key]
  (let [cipher (get-cipher Cipher/DECRYPT_MODE key)]
    (String. (.doFinal cipher (base64-decode text)))))

(def ^:dynamic *serialization-key* 
  "This is the key used to encrypt and decrypt the parameters. It should 
  be set to something unique in every application."
  "defaultkey")
(def ^:dynamic *obfuscated-parameter* 
  "This is the parameter name that the obfuscated parameters will be 
  passed under. For example, if this value is 'params', then 
  wrap-obfuscated-params will look for a parameter which looks like this 
  '?params=k2yeytowAcYW30tqZoCm4w==' to deserialize."
  "details")

(defn serialize 
  "Serializes and obfuscates the object value. Value can be any Clojure 
  form that does not need to be evaluated to be read."
  [value]
  (encrypt (pr-str value) *serialization-key*))

(defn deserialize 
  "Deserializes a value which had previously been serialized by serialize"
  [value]
  (read-string (decrypt value *serialization-key*)))

(defn serialize-link 
  "Simplistic function to create a url with a obfuscated parameter. Value
  will be serialized and added to link as a parameter. This function should
  be called from within a handler with the wrap-obfuscated-params middleware
  lest the wrong key or parameter name be used."
  [link value]
  (str link
       "?" 
       *obfuscated-parameter*
       "="
       (form-encode (serialize value))))

(defn wrap-obfuscate-params
  "Middleware to automatically deserialize an obfuscated parameter in a 
  request and make it available in the request map. The deserialized
  object is saved in the :deserialized-params field of the request map.
  If the deserialized object is map, it is merged into the :params
  field as well.

  It takes the following options:
    serialization-key - A string to encrypt and decrypt parameters
    obfuscated-parameter - The name of the parameter in which the 
                           obfuscated parameter is stored

  *serialization-key* and *obfuscation-parameter* will be bound to these 
  values."
  [handler & [serialization-key obfuscated-parameter]]
  (wrap-params
   (fn [request]
     (binding [*serialization-key* (or serialization-key *serialization-key*)
               *obfuscated-parameter* (or obfuscated-parameter *obfuscated-parameter*)]
       (let [{params :params} request
             deserialized-params (get params *obfuscated-parameter*)
             deserialized-params (when deserialized-params (deserialize deserialized-params))]
         (handler (assoc request 
                    :deserialized-params deserialized-params
                    :params (merge params (when (map? deserialized-params)
                                            deserialized-params)))))))))
