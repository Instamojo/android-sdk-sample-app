package lib

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"math/rand"
	"net/http"
	"net/url"
	"time"
)

const PROD_URL = "https://api.instamojo.com"
const TEST_URL = "https://test.instamojo.com"

var refundTypes = map[string]string{
	"RFD": "Duplicate/delayed payment.",
	"TNR": "Product/service no longer available.",
	"QFL": "Customer not satisfied.",
	"QNR": "Product lost/damaged.",
	"EWN": "Digital download issue.",
	"TAN": "Event was canceled/changed.",
	"PTH": "Problem not described above.",
}

type credentials struct {
	prodClientID     string
	prodClientSecret string
	testClientID     string
	testClientSecret string
}

var creds credentials

//SetCredentials will take in production and test credentials of the User
func SetCredentials(prodClientID, prodClientSecret, testClientID, testClientSecret string) {
	creds = credentials{
		prodClientID:     prodClientID,
		prodClientSecret: prodClientSecret,
		testClientID:     testClientID,
		testClientSecret: testClientSecret,
	}
}

//CreateOrderTokens will return necessary token in []byte format
func CreateOrderTokens(env string) ([]byte, error) {
	authUrl := PROD_URL
	id := creds.prodClientID
	secret := creds.prodClientSecret
	if env == "test" {
		authUrl = TEST_URL
		id = creds.testClientID
		secret = creds.testClientSecret
	}

	authUrl += "/oauth2/token/"
	values := url.Values{}
	values.Set("client_id", id)
	values.Set("client_secret", secret)
	values.Set("grant_type", "client_credentials")
	authRequest, err := http.NewRequest("POST", authUrl, bytes.NewBufferString(values.Encode()))
	if err != nil {
		return []byte(""), err
	}

	authRequest.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	client := &http.Client{}
	resp, err := client.Do(authRequest)
	if err != nil {
		return []byte(""), err
	}
	defer resp.Body.Close()

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return []byte(""), err
	}

	var jsonResponse struct {
		AccessToken   string `json:"access_token,omitempty"`
		Error         string `json:"error,omitempty"`
		TransactionID string `json:"transaction_id,omitempty"`
	}

	if err = json.Unmarshal(data, &jsonResponse); err != nil {
		return []byte(""), err
	}

	if jsonResponse.AccessToken != "" {
		jsonResponse.TransactionID = generateRandomString(15)
	}

	marshalledData, err := json.Marshal(jsonResponse)
	if err != nil {
		return []byte(""), err
	}

	return marshalledData, nil
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

//GetOrderStatus return the status of the order referencing either orderID or transactionID. Preference will be given to
//orderID
func GetOrderStatus(env, authorizationHeader, orderID, transactionID string) ([]byte, error) {
	statusURL := PROD_URL
	if env == "test" {
		statusURL = TEST_URL
	}

	statusURL += "/v2/gateway/orders/"
	if orderID == "" {
		statusURL += "transaction_id:" + transactionID + "/"
	} else {
		statusURL += "id:" + orderID + "/"
	}

	statusRequest, err := http.NewRequest("GET", statusURL, nil)
	if err != nil {
		return []byte(""), err
	}

	statusRequest.Header.Set("Authorization", authorizationHeader)
	statusRequest.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	client := &http.Client{}
	resp, err := client.Do(statusRequest)
	if err != nil {
		return []byte(""), err
	}

	data, err := ioutil.ReadAll(resp.Body)
	defer resp.Body.Close()

	if err != nil {
		return []byte(""), err
	}

	return data, nil
}

//InitiateRefund wil initiate refund for the paymentID for the given with given refund reason
//refundType should be within the following types
//RFD: Duplicate/delayed payment.
//TNR: Product/service no longer available.
//QFL: Customer not satisfied.
//QNR: Product lost/damaged.
//EWN: Digital download issue.
//TAN: Event was canceled/changed.
//PTH: Problem not described above.
func InitiateRefund(env, authorizationHeader, transactionID, amount, refundType, body string) (int, error) {
	refundURL := PROD_URL
	if env == "test" {
		refundURL = TEST_URL
	}

	if _, exist := refundTypes[refundType]; !exist {
		return http.StatusBadRequest, errors.New("Invalid refund type " + refundType)
	}

	data, err := GetOrderStatus(env, authorizationHeader, "", transactionID)
	if err != nil {
		return http.StatusInternalServerError, err
	}

	var jsonResponse struct {
		ID            string `json:"id"`
		TransactionID string `json:"transaction_id"`
		Payments      []struct {
			ID     string `json:"id"`
			Status string `json:"status"`
		} `json:"payments"`
		Success bool   `json:"success"`
		Message string `json:"message"`
	}

	if err := json.Unmarshal(data, &jsonResponse); err != nil {
		return http.StatusInternalServerError, err
	}

	if jsonResponse.Success || len(jsonResponse.Payments) < 1 {
		return http.StatusBadRequest, errors.New(jsonResponse.Message)
	}

	status := jsonResponse.Payments[0].Status
	paymentID := jsonResponse.Payments[0].ID

	if status != "successful" {
		return http.StatusBadRequest, errors.New("Cannot initiate refund for an Unsuccessful transaction")
	}

	refundURL += fmt.Sprintf("/v2/payments/%s/refund/", paymentID)
	params := url.Values{}
	params.Set("type", refundType)
	params.Set("refund_amount", amount)
	params.Set("body", body)

	refundRequest, err := http.NewRequest("POST", refundURL, bytes.NewBufferString(params.Encode()))
	if err != nil {
		return http.StatusInternalServerError, err
	}

	refundRequest.Header.Set("Authorization", authorizationHeader)
	refundRequest.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	client := &http.Client{}
	resp, err := client.Do(refundRequest)
	if err != nil {
		return http.StatusInternalServerError, err
	}
	_, err = ioutil.ReadAll(resp.Body)
	if err != nil {
		return http.StatusInternalServerError, err
	}

	return resp.StatusCode, nil
}
