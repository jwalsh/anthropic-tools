sequenceDiagram
    App->>Model: How many shares of General Motors can I buy with $500?
    Model->>App: {"stop_reason": "tool_use", "tool_use": {"name": "get_stock_price", "input": {"company": "General Motors"}}}
    App->>Stock Market API: get_stock_price("General Motors")
    Stock Market API->>App: {"symbol": "GM", "price": 43.09}
    App->>Model: {"role": "user", "content": [{"type": "tool_result", "tool_use_id": "...", "content": "{"symbol": "GM", "price": 43.09}"}]}
    Model->>App: It looks like with a current share price of $43.09, you can buy around 11 shares with your $500.
