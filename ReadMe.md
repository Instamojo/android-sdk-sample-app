# Android SDK Sample Documentation

## Why Sample App?
1. The Sample app implements the complete scope of the Android SDK. 
2. To help you kick start your own application using the Sample App as a Base so that you can focus 
on developing a cool application while we take care of payments in your application.

## What does Sample App do now?
1. Create orders and make payments on either Test or Production Environments.
2. Check for the Order status.
3. Refund the Payment for a completed Order.

## Flow of the Sample App?
1. Choose either Test or Production Environment
2. Give your details and proceed to create a new Order with Instamojo.
3. Choose from a Payment method from multiple Payment Methods.
4. Make the Payment.
5. Check the Order status.
6. If successful, initiate a full refund for that Order.

## What about Existing Projects?
Well, we got you covered there as well. Check out the Integration Documentation 
[here](https://docs.instamojo.com/page/android-sdk) to integrate Instamojo SDK in your Project.

## Requirements
From here on we assume that you have `Client ID` and `Client Secret` for Production as well as Test. 
'Why Test?' you might think. Test Environment to do the integration check of the SDK. 

If you do not have the credentials yet, raise a support ticket so that we could generate and 
send you the credentials in a jiffy.

## How to Generate Access Token?
Please check the Sample Server [documentation](https://github.com/Instamojo/sample-sdk-server/blob/master/Readme.md#generating-access-token) on how to generate access_token using `client_id` and `client_secret`

## What is this `Transaction ID` I keep hearing about?
Well, transaction ID is a unique ID for an Order. Using this transaction ID, 
you can fetch Order status, get order details, and even initiate a refund for the Order attached to that transaction ID.

The transaction ID should be unique for every Order.

## Well, is there any sample to get me started on the server side?
Yes, we do have a sample server written in Google Go. Sample uses the Sample server to get `access_token` and `transaction_id`
to create an `Order`.

You can check the documentation for the Sample Server [here](https://github.com/Instamojo/sample-sdk-server/blob/master/Readme.md)

## I have few more queries
Well, if this documentation doesn't answer any specific questions regarding the Sample App, please raise support ticket. We will respond ASAP.
