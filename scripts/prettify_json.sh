

for J in doc/*.json; do
	echo "Processing $J"
	cat $J | jq . > $J.tmp
	# Ccheck if the file is empty
	if [ ! -s $J.tmp ]; then
		echo "File $J is empty"
		rm $J.tmp
		continue
	fi
	mv $J.tmp $J
done
