// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.


import configUnionTypes.mod1;
import ballerina/jballerina.java;
import configUnionTypes.mod2;
import testOrg/configLib.mod1 as configLib;
import ballerina/test;

configurable configLib:HttpVersion & readonly httpVersion = ?;
configurable mod1:CountryCodes & readonly countryCode = ?;
configurable mod1:CountryCodes[] countryCodes = ?;

type HttpResponse record {|
    configLib:HttpVersion httpVersion;
|};

configurable HttpResponse httpResponse = ?;
configurable mod1:Country country = ?;

configurable anydata anydataVar = ?;
configurable int|string intStringVar = 2;
configurable anydata[] anydataArray = ?;
configurable map<anydata> anydataMap = ?;
configurable table<map<anydata>> anydataTable = ?;

configurable configLib:One|configLib:Two|configLib:Three number = ?;
configurable configLib:GrantConfig config1 = ?;
configurable configLib:GrantConfig config2 = ?;
configurable configLib:GrantConfig config3 = ?;

public function main() {
    testEnumValues();
    mod2:testEnumValues();
    print("Tests passed");
}

function testEnumValues() {
    test:assertEquals(httpVersion, configLib:HTTP_1_1);
    test:assertEquals(countryCode, mod1:SL);
    test:assertEquals(httpResponse.httpVersion, configLib:HTTP_2);
    test:assertEquals(country.countryCode, mod1:US);
    test:assertEquals(countryCodes[0], mod1:US);
    test:assertEquals(countryCodes[1], mod1:SL);
    test:assertEquals(anydataVar, "hello");
    test:assertEquals(intStringVar, 12345);
    test:assertEquals(anydataArray.toString(), "[\"hello\",1,2,3.4,false]");
    test:assertEquals(anydataMap.toString(), "{\"username\":\"waruna\",\"age\":14,\"marks\":85.67,\"isAdmin\":true}");
    test:assertEquals(anydataTable.toString(), "[{\"username\":\"manu\",\"age\":12,\"marks\":123.456,\"isAdmin\":false},{\"username\":\"hinduja\",\"age\":16,\"marks\":98.76,\"isAdmin\":true}]");
    test:assertEquals(number.toString(), "three");
    test:assertEquals(config1.toString(), "{\"clientId\":123456,\"clientSecret\":\"hello\",\"clientConfig\":{\"httpVersion\":\"HTTP_1_1\",\"customHeaders\":{\"header1\":\"header1\",\"header2\":\"header2\"}}}");
    test:assertEquals(config2.toString(), "{\"token\":\"123456\",\"timeLimit\":12.5,\"clientConfig\":{\"httpVersion\":\"HTTP_2\",\"customHeaders\":{\"header3\":\"header3\",\"header4\":\"header4\"}}}");
    test:assertEquals(config3.toString(), "{\"password\":[\"1\",2,3]}");
}

function print(string value) {
    handle strValue = java:fromString(value);
    handle stdout1 = stdout();
    printInternal(stdout1, strValue);
}

public function stdout() returns handle = @java:FieldGet {
    name: "out",
    'class: "java/lang/System"
} external;

public function printInternal(handle receiver, handle strValue) = @java:Method {
    name: "println",
    'class: "java/io/PrintStream",
    paramTypes: ["java.lang.String"]
} external;
