### Before you start

Download this text file (1.2Mb) at the root of the project.
```bash
$curl https://www.gutenberg.org/files/59857/59857-0.txt -o 59857.txt
```

### Running 

Run the server
```bash
# from the project root directory
$mvn clean install -N

# go to the server directory
$cd server
$mvn clean install -DskipTests
$mvn exec:java -Dexec.mainClass="org.example.files.server.FileServer" -Dexec.args="../"
```

Run the client in a different console
```bash
$cd client
$mvn clean package -DskipTests
$mvn exec:java -Dexec.mainClass="org.example.files.client.CommandLine" -Dexec.args="-d /tmp -h localhost -p 49999"

>> a
Unknown command
>> index
file_list_here
...
>> get a
ERROR
No file found
>> get ../59857.txt
OK: writing to /tmp/59857.txt
>> q
Good bye
```

To see how the server can handle multiple clients, see <code>org.example.files.client.handler.MultiClientTest</code>
