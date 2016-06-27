# Sample SDK Server Documentation

## Why Sample Server project?
1. To serve as a backend for the Sample App. 
You can find the documentation for the Sample App [here](../ReadMe.md)
2. To help you get started with the backend for your Application.

## Scope of the project
The project currently implements the following features:<br>
1. Generate `access_token` and unique `transaction_id`. This will be called before a new order is created on Sample App.
2. Getting Order Details of an `Order` attached to the `transaction_id` or `order_id`.
3. Initiate refund for the `Order` attached to the `transaction_id`.

## Generating Access Token
Generating the access_token is a `HTTP POST` request.
You need to generate this on your server and send it to client.

The post parameters are as follows:<br>
`"client_id":"YOUR CLIENT ID"`<br>
`"client_secret":"YOUR CLIENT SECRET"`<br>
`"grant_type": "client_credentials"`<br>

URL for production - "https://api.instamojo.com/oauth2/token/"<br>
URL for test - "https://test.instamojo.com/oauth2/token/"

### Successful Response
```JSON
{
  "access_token": "y70kak2K0Rg7J4PAL8sdW0MutnGJEl",
  "token_type": "Bearer",
  "expires_in": 36000,
  "scope": "read write"
}
```

### Failed Responses
```JSON
{
  "error": "unsupported_grant_type"
}
```

```JSON
{
  "error": "invalid_client"
}
```

Example code for this post request can be found [here](lib/core.go#L48).

## Generating Transaction ID
The Sample Server generates a random string of length 15. Since, the server doesn't have any database attached to store data, 
it doesn't store the transaction ID.
But you would need to implement the database logic to store the `transaction_id` and other relevant information before creating the `Order` for later checks.

## Getting `Order` details
Getting specific Order details is a `HTTP GET` request.<br>
The following are the mandatory headers to be passed along with the request.<br>
1. `"Content-Type":"application/x-www-form-urlencoded"`
2. `"Authorization", "Bearer <Access Token>"`

Example code for this request can be found [here](lib/core.go#L115).

## Initiating Refund for a particular `Order`
Initiating Refund is a `HTTP POST` request with following mandatory post parameters as well as headers:<br>

### Headers
1. `"Content-Type":"application/x-www-form-urlencoded"`
2. `"Authorization", "Bearer <Access Token>"`

### Post Params
1. `"type":"REFUND TYPE"` should be with in the following types:<br>
    `RFD: Duplicate/delayed payment.`<br>
    `TNR: Product/service no longer available.`<br>
    `QFL: Customer not satisfied.`<br>
    `QNR: Product lost/damaged.`<br>
    `EWN: Digital download issue.`<br>
    `TAN: Event was canceled/changed.`<br>
    `PTH: Problem not described above.`<br>
    
2. `"refund_amount":"Amount"` Should be with in 0 and original `Order` amount<br>

3. `"body":"Reason for refund"`

Example code for refund can be found [here](lib/core.go#L161).

## I have few other queries
If this documentation didn't answer all your queries, do raise a support ticket. Will will respond ASAP.


