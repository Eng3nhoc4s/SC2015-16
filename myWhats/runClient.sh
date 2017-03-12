#!/bin/bash
java -cp bin/ -Djava.security.manager -Djava.security.policy="client.policy" client.myWhats $@
