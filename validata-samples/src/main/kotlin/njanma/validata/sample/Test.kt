package njanma.validata.sample

import njanma.validata.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        dslTest()
    }

    fun dslTest() {
        val service = Service()

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


        val userProfile = UserProfile(1, "gena", 2, "asds", RegistrationData(Instant.EPOCH, Device.TV, UUID.randomUUID()))
        val validated: ValidationResult<String> = userProfileValidation(userProfile)
        println(validated)
    }

    data class UserProfile(val id: Long,
                           val name: String,
                           val age: Int?,
                           val email: String,
                           val registration: RegistrationData)

    enum class Device {
        PC, MOBILE, TV
    }

    data class RegistrationData(val issued: Instant, val device: Device, val deviceUUID: UUID)

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
}