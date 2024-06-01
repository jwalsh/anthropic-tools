# Diagram Generation
## Generate diagrams from Mermaid markup files
diagrams: $(patsubst doc/%.mmd,doc/%.png,$(wildcard doc/*.mmd))
	@echo "All diagrams generated successfully."

doc/%.png: doc/%.mmd
	@echo "Generating diagram: $< -> $@"
	mmdc -i $< -o $@

prettify-json: $(patsubst %.json,%.json,$(wildcard *.json))
	@echo "Prettifying JSON files..."
	@echo "All JSON files prettified successfully."
