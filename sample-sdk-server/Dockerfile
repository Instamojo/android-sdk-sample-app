FROM alpine:latest

# Copy the executable.
ADD ./sample-sdk-server /

# Get certificates since we need for http requests
RUN apk add --update ca-certificates
RUN update-ca-certificates

# Do not run as root.
USER nobody

# Run the main command by default when the container starts.
ENTRYPOINT ["/sample-sdk-server"]

# Expose port 8080 for the REST APIs.
EXPOSE 8080