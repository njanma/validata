package njanma.validata.sample

import njanma.validata.*
import kotlin.text.contains

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        dslTest()
    }

    fun dslTest() {
        val service = Service()

        val haveRealFullName = Matcher<UserName, String> {
            MatcherResult(it.name.contains("real"), "name is unreal!", "name is too real!")
        }
        val userNameValidator = Validator<UserName, String> {
            UserName::name {
                service::checkName.shouldBe(false, { "should be false but was returned: $it" })
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
                service::findUserName checkedBy userNameValidator
                service::findUserName shouldNot haveRealFullName
                service::findByEmail ifPresent {
                    checkedBy(profileEmailValidator)
                }
            }

            UserProfile::age ifPresent {
                moreThan(18, "age should be more than 18")
                service::checkAge.shouldBe(true, { "age should be > 18" })
            }
            UserProfile::name should haveRealFullName
            UserProfile::name checkedBy Validator {
                UserName::name {
                    service::checkName.shouldBe(false, "name not allowed")
                }
            }
        }


        val userProfile = UserProfile(UserName(1, "gena"), 2, "asds")
        val validated: ValidationResult<String> = userProfileValidation(userProfile)
        println(validated)
    }

    data class UserProfile(val name: UserName,
                           val age: Int?,
                           val email: String)

    data class UserName(val id: Long, val name: String)

    class Service {
        fun checkUserProfile(userProfile: UserProfile): Boolean = true
        fun checkAge(age: Int): Boolean = age >= 18
        fun checkName(name: String): Boolean = true
        fun findByEmail(email: String): UserProfile? = UserProfile(UserName(1, "realGena"), 3, "fdhfjshf")
        fun findByUserName(userName: UserName): UserProfile? = null
        fun findUserName(email: String): UserName = UserName(1, "realGena")
    }
}