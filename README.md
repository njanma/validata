[ ![Download](https://api.bintray.com/packages/njanma/validata/validata-core/images/download.svg) ](https://bintray.com/njanma/validata/validata-core/_latestVersion)

Validata
========

This version of the document is for version 0.1+.

Key points
----------
+ Type-safe DSL
+ Zero dependencies
+ Highly customizable

Quickstart
----------

### Intall

#### Gradle
Add the `Validata` bintray repository to your build.gradle
```groovy
repositories {
    maven {
        url "https://dl.bintray.com/njanma/validata"
    }
}
```
Add dependency
```groovy
compile "io.validata:validata-core:0.1.0"
```

#### Maven
Add the `Validata` bintray repository to your Maven settings

```xml
<repository>
    <id>bintray-njanma-validata</id>
    <name>bintray</name>
    <url>https://dl.bintray.com/njanma/validata</url>
</repository>
```
Add dependency
```xml
<dependency>
    <groupId>io.validata</groupId>
    <artifactId>validata-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

Examples
--------
This section contains a few examples.

Suppose you have a complex data class:

```kotlin
data class UserProfile(val id: Long,
                       val name: String,
                       val age: Int?,
                       val email: String,
                       val registration: RegistrationData)
                       
enum class Device {
    PC, MOBILE, TV
}

data class RegistrationData(val issued: Instant, val device: Device, val deviceUUID: UUID)                       
```
and service for working with him:

```kotlin
class Service {
    fun checkUserProfile(userProfile: UserProfile): Boolean = true
    fun checkAge(age: Int): Boolean = age >= 18
    fun checkName(name: String): Boolean = true
    fun checkDevice(device: Device): Boolean = true
    fun checkUUID(uuid: UUID): Boolean = true
    fun findByEmail(email: String): UserProfile? =
            UserProfile(1, "realGena", 3, "gena999@mail.com",
                    RegistrationData(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC), Device.MOBILE, UUID.randomUUID()))

    fun findByDeviceUUID(uuid: UUID): UserProfile? = null
    fun findRegistrationData(email: String): RegistrationData =
            RegistrationData(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC), Device.PC, UUID.randomUUID())
}
```
Using `Validata` you can quickly write some validation rules, for example:
```kotlin
val service = Service()

val profileEmailValidator = Validator<UserProfile, String> {
        UserProfile::email {
            notBlank("email should be not blank")
            minLength(99)
            maxLength(200)
        }
        UserProfile::registration {
            service::checkDevice shouldBe false
        }
        UserProfile::age ifPresent {
            moreThan(18)
        }
}
```
and apply it to your data
```kotlin
val userProfile = UserProfile(1, "gena", 2, "incorrectEmail.com", RegistrationData(Instant.EPOCH, Device.TV, UUID.randomUUID()))
val validated: ValidationResult<String> = userProfileValidation(userProfile)
```
`ValidationResult` can be one of these types: `Valid` or `Invalid<E>`. 
`Invalid<E>` result has a sequence of errors that were defined earlier.

#### If present
```kotlin
val userProfileValidation = Validator<UserProfile, String> {
    UserProfile::age ifPresent {
        moreThan(18, "age should be more than 18")
    }
}
```
Without `ifPresent` it won't be compile, because the field age can be nullable.

#### Checked by
```kotlin
val userRegistrationValidator = Validator<RegistrationData, String> {
    RegistrationData::device {
        service::checkDevice shouldBe false
    }
}
val userProfileValidation = Validator<UserProfile, String> {
    UserProfile::email{
        service::findRegistrationData checkedBy userRegistrationValidator
    }
}    
```
#### Matcher
```kotlin
val profileEmailValidator = Validator<UserProfile, String> {
    UserProfile::email should hasLength(1)
}    
```

#### Custom matcher
```kotlin
val haveRealRegistrationDate = Matcher<RegistrationData, String> {
    MatcherResult(it.issued.isBefore(Instant.now()), "issued date should be in past!", "issued date should be in future")
}

val userProfileValidation = Validator<UserProfile, String> {
    UserProfile::email{
        service::findRegistrationData shouldNot haveRealRegistrationDate
    }
}
``` 

Complex example
---------------

```kotlin
val haveRealRegistrationDate = Matcher<RegistrationData, String> {
    MatcherResult(it.issued.isBefore(Instant.now()), "issued date should be in past!", "issued date should be in future")
}
val userRegistrationValidator = Validator<RegistrationData, String> {
    RegistrationData::device {
        service::checkDevice.shouldBe(false, { "should be false but was returned: $it" })
    }
}
val userCheckMsg = "user check invalid"
val profileEmailValidator = Validator<UserProfile, String> {
    UserProfile::email{
        notBlank("email should be not blank")
        minLength(99, "email should has length more than 999", "email should has length less than 999")
        maxLength(200)
    }
    UserProfile::email should hasLength(1).transform("email should has length 1", "email shouldn't has length 1")
    UserProfile::age.shouldNotBe(3, { userCheckMsg }) {
        println("user check invalid")

    }
    UserProfile::email.checkedIf(true) {
        minLength(999, "error on checkedIf", "")
    }
}

val userProfileValidation = Validator<UserProfile, String> {
    UserProfile::email{
        service::findRegistrationData checkedBy userRegistrationValidator
        service::findRegistrationData shouldNot haveRealRegistrationDate
        service::findByEmail ifPresent {
            checkedBy(profileEmailValidator)
        }
    }

    UserProfile::age ifPresent {
        moreThan(18, "age should be more than 18")
        service::checkAge.shouldBe(true, { "age should be > 18" })
    }
    UserProfile::registration should haveRealRegistrationDate
    UserProfile::registration checkedBy Validator {
        RegistrationData::deviceUUID {
            service::checkUUID.shouldBe(false, "name not allowed")
        }
    }
}

val userProfile = UserProfile(1, "gena", 2, "test@mail.com", RegistrationData(Instant.EPOCH, Device.TV, UUID.randomUUID()))
val validated: ValidationResult<String> = userProfileValidation(userProfile)
```