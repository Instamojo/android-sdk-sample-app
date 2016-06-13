package main

import (
	"bytes"
	"log"
	"net/http"
	"net/url"

	"encoding/json"

	"math/rand"
	"time"

	"io/ioutil"

	"github.com/gorilla/mux"
)

func main() {

	router := mux.NewRouter()
	router.HandleFunc("/create", createOrderTokens)

	log.Fatal(http.ListenAndServe(":8080", LoggingHandler(router)))
}

func createOrderTokens(w http.ResponseWriter, r *http.Request) {
	const CLIENT_ID = "cNrgex0RQ3P176F0jCjFfEyCy2UnXjunM1AZCIT8"
	const CLIENT_SECRET = "SEqtkfR4GriSPtZkwgBWKEEYCpA8nxa7Q8bDRHqJSWEX1nPyTdNL8hglzYYNvI6kCVGlLr7abPWZ0L9S77VwpBDUTGdaSM9EdZdatQQjmmeykTlyyMqiNuSQs6N6WBsW"
	const PROD_URL = "https://api.instamojo.com/oauth2/token/"

	const CLIENT_ID_TEST = "tFGpLdAwsLEQPQ5KmxJPzvLNDKGkY7wk3F6xbIAK"
	const CLIENT_SECRET_TEST = "R0XbUQTMztT23diy4DKTEqLW4HSTLMrEwgya1jDWX8jKUESg0Ljzr7qNfxZCMtjTuNnRvPYv1fJNJ6bm8DaGDCJ63L8y6W7RRhsBX8f2mws7ZNMnWz4PaxFWddMJZGLr"
	const TEST_URL = "https://test.instamojo.com/oauth2/token/"

	if err := r.ParseForm(); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	mode := r.PostForm.Get("mode")
	authUrl := PROD_URL
	clientID := CLIENT_ID
	clientSecret := CLIENT_SECRET
	if mode == "test" {
		authUrl = TEST_URL
		clientID = CLIENT_ID_TEST
		clientSecret = CLIENT_SECRET_TEST
	}

	values := url.Values{}
	values.Set("client_id", clientID)
	values.Set("client_secret", clientSecret)
	values.Set("grant_type", "client_credentials")
	authRequest, err := http.NewRequest("POST", authUrl, bytes.NewBufferString(values.Encode()))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	authRequest.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	client := &http.Client{}
	resp, err := client.Do(authRequest)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer resp.Body.Close()

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	var jsonReponse struct {
		AccessToken   string `json:"access_token,omitempty"`
		Error         string `json:"error,omitempty"`
		TransactionID string `json:"transaction_id,omitempty"`
	}

	if err = json.Unmarshal(data, &jsonReponse); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	if jsonReponse.AccessToken != "" {
		jsonReponse.TransactionID = generateRandomString(15)
	}

	marshalledData, err := json.Marshal(jsonReponse)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.Write(marshalledData)
}

func generateRandomString(strlen int) string {
	rand.Seed(time.Now().UTC().UnixNano())
	const chars = "abcdefghijklmnopqrstuvwxyz0123456789"
	result := make([]byte, strlen)
	for i := 0; i < strlen; i++ {
		result[i] = chars[rand.Intn(len(chars))]
	}
	return string(result)
}
