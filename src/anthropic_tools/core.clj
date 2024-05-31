(ns anthropic-tools.core
  (:require [clojure.spec.alpha :as s]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(defrecord AnthropicPayload [model max-tokens tools messages]
  model
  s/Str
  
  max-tokens
  s/Int
  
  tools
  [(s/map-of keyword? any?)]
  
  messages
  [(s/map-of keyword? any?)])

(defrecord AnthropicResponse [id model stop-reason role content]
  id
  s/Str
  
  model
  s/Str
  
  stop-reason
  s/Str
  
  role
  s/Str
  
  content
  [(s/conditional
     #(= (:type %) "text")
     (s/keys :req-un [::type ::text])
     
     #(= (:type %) "tool_use")
     (s/keys :req-un [::type ::id ::name ::input]))])

(s/def ::type string?)
(s/def ::text string?)
(s/def ::id string?)
(s/def ::name string?)
(s/def ::input (s/map-of keyword? any?))

(defn send-message
  [api-key payload]
  (let [url "https://api.anthropic.com/v1/messages"
        headers {"content-type" "application/json"
                 "x-api-key" api-key
                 "anthropic-version" "2023-06-01"}
        response (http/post url
                            {:headers headers
                             :body (json/generate-string payload)})]
    (if (= (:status response) 200)
      (json/parse-string (:body response) true)
      (throw (ex-info "Error sending message to Anthropic API"
                      {:status (:status response)
                       :body (:body response)})))))

(defn -main
  "Anthropic Tools."
  [& args]
  (let [api-key (System/getenv "ANTHROPIC_API_KEY")
        payload (->AnthropicPayload
                  "claude-v1.3"
                  1024
                  [{:name "get_weather"
                    :description "Get the current weather in a given location"
                    :input_schema {:type "object"
                                   :properties {:location {:type "string"
                                                           :description "The city and state, e.g. San Francisco, CA"}}
                                   :required [:location]}}]
                  [{:role "user"
                    :content "What is the weather like in San Francisco?"}])
        response (send-message api-key payload)]
    (println "API Response:")
    (println response)))
