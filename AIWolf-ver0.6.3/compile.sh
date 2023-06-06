find -d clients_java -name "*.java" > java_list.txt
javac -cp 'lib/aiwolf/*:clients_java/' @java_list.txt
rm java_list.txt