KOTLINC=kotlinc
JAVA=java
INKSCAPE=inkscape
CONVERT=convert
PDFTK=pdftk

SOURCE_APP=app.kt
BIN=bin
JAR_APP=$(BIN)/app.jar
OUTPUT=output

SOURCES=$(shell seq -w 1 100)
FRONTS=$(patsubst %, $(OUTPUT)/%-front.svg, $(SOURCES))
BACKS=$(patsubst %, $(OUTPUT)/%-back.svg, $(SOURCES))
ALL=$(sort $(FRONTS) $(BACKS))
ALL_PDF=$(ALL:.svg=.pdf)
PDF=$(OUTPUT)/cards.pdf

all: $(PDF)

$(OUTPUT):
	mkdir $(OUTPUT)

$(BIN):
	mkdir $(BIN)

$(JAR_APP): $(SOURCE_APP) $(BIN)
	$(KOTLINC) $< -include-runtime -d $@

$(OUTPUT)/%-front.svg: $(JAR_APP) $(OUTPUT)
	$(JAVA) -jar $(JAR_APP) -- $(subst -front.svg,,$(subst $(OUTPUT)/,,$@)) front $@

$(OUTPUT)/%-back.svg: $(JAR_APP) $(OUTPUT)
	$(JAVA) -jar $(JAR_APP) -- $(subst -back.svg,,$(subst $(OUTPUT)/,,$@)) back $@

$(OUTPUT)/%.png: $(OUTPUT)/%.svg
	$(INKSCAPE) --export-dpi=600 --export-png=$@ $^

$(OUTPUT)/%.pdf: $(OUTPUT)/%.png
	$(CONVERT) $^ $@

$(PDF): $(ALL_PDF)
	$(PDFTK) $^ cat output $@

clean:
	rm -rf $(OUTPUT)
	rm -rf $(BIN)

.PHONY: all clean
