### Problem statement

Design and implement a RESTful API (including data model and the backing implementation) for money transfers between accounts.

#### Toolset:

Java, Gradle, Javalin, Junit

#### How to build

./gradlew clean test shadowJar

#### How to run standalone app

java -jar build/libs/money-transfer-assignment-0.1-all.jar

#### How to test app

The application runs on http://localhost:7000/

The REST api collection can be found here: https://documenter.getpostman.com/view/2226034/SW7XbVaX?version=latest

##### PS: Unit tests are self explanatory including concurrent access tests.


