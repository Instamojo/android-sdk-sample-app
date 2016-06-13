install:
	go get github.com/tools/godep

package:
	go clean
	CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o sample-sdk-server .

all: install package
