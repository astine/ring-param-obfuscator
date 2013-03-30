(ns ring-param-obfuscator.core-test
  (:use clojure.test
        ring.mock.request
        [ring.util.response :only [response]]
        ring.util.codec
        ring-param-obfuscator.core))

(defn random-string [length]
  (apply str (repeatedly length #(char (+ 32 (rand-int 94))))))

(deftest basic-encyptor
  (let [key (random-string 10)
        test-data1 (random-string 10)
        test-data2 (random-string 100)
        test-data3 (random-string 1000)]
    (is (= "foo" (decrypt (encrypt "foo" key) key)))
    (is (= "123" (decrypt (encrypt "123" key) key)))
    (is (= "fWf8wk3SD" (decrypt (encrypt "fWf8wk3SD" key) key)))
    (is (= test-data1 (decrypt (encrypt test-data1 key) key)))
    (is (= test-data2 (decrypt (encrypt test-data2 key) key)))
    (is (= test-data3 (decrypt (encrypt test-data3 key) key)))))

(deftest basic-serializer
  (let [test-data1 (random-string 10)
        test-data2 (random-string 100)
        test-data3 (random-string 1000)]
    (is (= "foo" (deserialize (serialize "foo"))))
    (is (= test-data1 (deserialize (serialize test-data1))))
    (is (= test-data2 (deserialize (serialize test-data2))))
    (is (= test-data3 (deserialize (serialize test-data3))))
    (binding [*serialization-key* (random-string 10)]
      (is (= "foo" (deserialize (serialize "foo"))))
      (is (= test-data1 (deserialize (serialize test-data1))))
      (is (= test-data2 (deserialize (serialize test-data2))))
      (is (= test-data3 (deserialize (serialize test-data3)))))))

(deftest serialized-linker
  (is (= "/?details=iChRjT23OFVFtAd0k7jfYg%3D%3D"
         (serialize-link "/" {:a 1}))))

(defn simple-handler [req]
  (response (pr-str (:deserialized-params req))))

(defn create-basic-request [test-data]
  (request :GET (serialize-link "/foo/10" test-data)))

(deftest test-wrapper
  (let [test-data1 (random-string 10)
        test-data2 (random-string 100)
        test-data3 (random-string 1000)
        test-data4 {:a (rand) :b test-data1 :c test-data3}]

    (let [app (wrap-obfuscate-params simple-handler)]
      (is (= (pr-str test-data1)
             (:body (app (create-basic-request test-data1)))))
      (is (= (pr-str test-data2)
             (:body (app (create-basic-request test-data2)))))
      (is (= (pr-str test-data3)
             (:body (app (create-basic-request test-data3)))))
      (is (= (pr-str test-data4)
             (:body (app (create-basic-request test-data4))))))

    (let [new-key (random-string 10)
          app (wrap-obfuscate-params simple-handler new-key)]
      (binding [*serialization-key* new-key]
        (is (= (pr-str test-data1)
               (:body (app (create-basic-request test-data1)))))
        (is (= (pr-str test-data2)
               (:body (app (create-basic-request test-data2)))))
        (is (= (pr-str test-data3)
               (:body (app (create-basic-request test-data3)))))
        (is (= (pr-str test-data4)
               (:body (app (create-basic-request test-data4)))))))
      
    (let [new-key (random-string 10)
          new-param "foobar"
          app (wrap-obfuscate-params simple-handler new-key new-param)]
      (binding [*serialization-key* new-key
                *obfuscated-parameter* new-param]
        (is (= (pr-str test-data1)
               (:body (app (create-basic-request test-data1)))))
        (is (= (pr-str test-data2)
               (:body (app (create-basic-request test-data2)))))
        (is (= (pr-str test-data3)
               (:body (app (create-basic-request test-data3)))))
        (is (= (pr-str test-data4)
               (:body (app (create-basic-request test-data4)))))))))
  
