#!/usr/bin/env bash

# Check if the ANTHROPIC_API_KEY environment variable is set
if [ -z "$ANTHROPIC_API_KEY" ]; then
	echo "The ANTHROPIC_API_KEY environment variable is not set. Please set it to your API key."
	exit 1
fi

curl https://api.anthropic.com/v1/messages \
	--header "x-api-key: $ANTHROPIC_API_KEY" \
	--header "anthropic-version: 2023-06-01" \
	--header "content-type: application/json" \
	--data \
	'{
    "model": "claude-3-opus-20240229",
    "max_tokens": 1024,
    "tools": [{
        "name": "get_weather",
        "description": "Get the current weather in a given location",
        "input_schema": {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "The city and state, e.g. San Francisco, CA"
                },
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "The unit of temperature, either 'celsius' or 'fahrenheit'"
                }
            },
            "required": ["location"]
        }
    },
    {
        "name": "get_time",
        "description": "Get the current time in a given time zone",
        "input_schema": {
            "type": "object",
            "properties": {
                "timezone": {
                    "type": "string",
                    "description": "The IANA time zone name, e.g. America/Los_Angeles"
                }
            },
            "required": ["timezone"]
        }
    }],
    "messages": [{
        "role": "user",
        "content": "What is the weather like right now in Boston? Also what time is it there?"
    }]
}' | jq .
 
 
