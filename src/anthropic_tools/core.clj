(ns anthropic-tools.core
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.string :as str]            
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

(defn calculator [operation operand1 operand2]
  (case operation
    "add" (+ operand1 operand2)
    "subtract" (- operand1 operand2)
    "multiply" (* operand1 operand2)
    "divide" (/ operand1 operand2)
    "exponent" (Math/pow operand1 operand2)))

(defn generate-wikipedia-reading-list [research-topic article-titles]
  (let [wikipedia-articles
        (for [t article-titles
              :let [results (wikipedia/search t)]
              :when (seq results)]
          (try
            (let [page (wikipedia/page (first results))
                  title (.getTitle page)
                  url (.getUrl page)]
              {:title title :url url})
            (catch Exception _ nil)))]
    (add-to-research-reading-file wikipedia-articles research-topic)))

(defn add-to-research-reading-file [articles topic]
  (with-open [file (io/writer "output/research_reading.md" :append true)]
    (.write file (str "## " topic "\n"))
    (doseq [article articles]
      (let [title (:title article)
            url (:url article)]
        (.write file (str "* [" title "](" url ")\n"))))
    (.write file "\n\n")))

(def wikipedia-tool
  {:name "wikipedia_search"
   :description "Searches Wikipedia for articles related to the given topic."
   :input_schema
   {:type "object"
    :properties
    {:research_topic {:type "string"
                      :description "The topic to search for on Wikipedia"}
     :article_titles {:type "array"
                      :items {:type "string"}
                      :description "A list of potential Wikipedia article titles related to the topic"}}
    :required ["research_topic" "article_titles"]}})

(defn get-research-help [topic num-articles]
  (let [messages [{:role "user"
                   :content (str "Please generate a list of " num-articles " potential Wikipedia article titles related to the topic '" topic "'. Do not include any URLs, only the article titles.")}]
        response (anthropic/messages-create
                   {:model "claude-3-haiku-20240307"
                    :system "You have access to tools, but only use them when necessary. If a tool is not required, respond as normal."
                    :messages messages
                    :max_tokens 500
                    :tools [wikipedia-tool]})]
    (if (= "tool_use" (:stop_reason response))
      (let [tool-use (last (:content response))
            tool-name (:name tool-use)
            tool-input (:input tool-use)]
        (when (= "wikipedia_search" tool-name)
          (println "Claude wants to use the Wikipedia search tool")
          (let [research-topic (:research_topic tool-input)
                article-titles (:article_titles tool-input)]
            (generate-wikipedia-reading-list research-topic article-titles))))
      (do
        (println "Claude didn't want to use a tool")
        (println "Claude responded with:")
        (println (-> response :content first :text))))))

;; Example usage
;; (get-research-help "Pirates Across The World" 7)
;; (get-research-help "History of Hawaii" 3)
;; (get-research-help "are animals conscious?" 3)

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
