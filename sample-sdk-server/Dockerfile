FROM alpine:latest

# Copy the executable.
ADD ./sample-sdk-server ./

# Do not run as root.
USER nobody

# Run the main command by default when the container starts.
ENTRYPOINT ["sample-sdk-server"]

# Expose port 8080 for the REST APIs.
EXPOSE 8080