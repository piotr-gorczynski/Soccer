Programmatic Initialization of Firebase Authentication in 
Google Cloud CI/CD Pipelines 

1. Introduction 

The implementation of Continuous Integration and Continuous Delivery (CI/CD) 
pipelines in Google Cloud often involves the automated deployment of various 
infrastructure components, including Firebase. A user has reported encountering an 
issue while attempting to enable Firebase Authentication programmatically within 
such a pipeline. Specifically, the use of a curl command targeting the Firebase Identity 
Toolkit API to enable email sign-in resulted in a "CONFIGURATION_NOT_FOUND" error. 
This error persisted until a manual intervention was performed by navigating to the 
Firebase console and clicking the “Get started” button within the Authentication tab. 
This action inexplicably allowed the subsequent programmatic configuration to 
succeed. This report aims to identify a programmatic method that replicates the initial 
setup performed by the “Get started” button in the Firebase console, thereby 
enabling the full automation of Firebase Authentication configuration within CI/CD 
pipelines. 

2. Understanding the "CONFIGURATION_NOT_FOUND" Error 

The "CONFIGURATION_NOT_FOUND" error arises within the context of the Firebase 
Identity Toolkit API, which serves as a mechanism to programmatically manage 
various aspects of Firebase Authentication. The user's attempt to use a PATCH 
request against the /config endpoint to enable email sign-in suggests an interaction 
with the core settings of the Authentication service. The error message itself strongly 
implies that the system is unable to locate a configuration that it expects to exist 
before the requested modification can be applied. This behavior directly correlates 
with the user's experience, where a manual step in the Firebase console was a 
necessary precursor to the successful execution of the same API call.1 

Analysis of online discussions surrounding this error provides valuable context. A 
Stack Overflow thread addressing the "CONFIGURATION_NOT_FOUND" error in the 
Google Identity Toolkit indicates that this issue can occur if authentication has not yet 
been enabled within the Firebase console.1 One user specifically noted that simply 
visiting the authentication page and clicking "Get Started" resolved the problem. This 
suggests that this manual action triggers an underlying initialization or provisioning 
process for the Firebase Authentication service that is a prerequisite for further 
configuration via the API. 

Another user encountered a similar error even after enabling the Identity Toolkit API at 
the Google Cloud project level.2 The resolution in this case also involved a manual step 
within the Google Cloud console to enable the Identity Provider. This further 
emphasizes that enabling the core API might not be sufficient for the Firebase 
Authentication service to be ready for configuration through its specific endpoints. 
There appears to be a distinct activation or initialization required within the Firebase 
Authentication context itself. The collective evidence points to the conclusion that the 
"CONFIGURATION_NOT_FOUND" error signifies that Firebase Authentication, 
particularly the email/password sign-in feature, demands an initial enabling or 
provisioning step that goes beyond basic project creation or enabling the Identity 
Toolkit API. The manual interaction with the “Get started” button in the Firebase 
console likely performs this crucial initial setup. 

3. Investigating Programmatic Initialization Options 

To identify a programmatic solution, several potential avenues were explored, 
including the Firebase Admin SDK, the Google Cloud CLI (gcloud), and the Firebase 
Identity Toolkit API itself. 

3.1. Firebase Admin SDK 

The Firebase Admin SDK offers a suite of libraries designed for server-side 
interactions with Firebase services. These libraries provide functionalities for 
managing users, handling authentication tokens, and implementing custom 
authentication flows.3 While the Admin SDK enables a wide range of authentication 
management tasks, the documentation and available resources do not indicate a 
specific function or method dedicated to performing the initial enabling or "Get 
started" action for Firebase Authentication.3 The focus of the Admin SDK appears to 
be on managing authentication processes after the service has already been 
initialized and is active. Snippets detailing the setup and initialization of the Admin 
SDK 6 illustrate how to connect to a Firebase project but do not reveal any methods 
for enabling the Authentication service itself. Therefore, the Firebase Admin SDK, in its 
current form, does not seem to offer a direct programmatic way to replicate the "Get 
started" functionality observed in the Firebase console. 

3.2. Google Cloud CLI (gcloud) 

The Google Cloud CLI (gcloud) is the primary command-line interface for interacting 
with Google Cloud services, including Firebase. While the gcloud CLI provides 
extensive capabilities for managing various Firebase resources, a direct command 

such as gcloud firebase auth enable does not appear to exist.10 However, research 
into related areas within the gcloud ecosystem provides some potential leads. One 
snippet discusses the use of the Firebase Admin SDK within a Quarkus application 
and mentions a configuration property (quarkus.google.cloud.firebase.auth.enabled) 
that enables Firebase Authentication.11 The existence of such a configuration flag 
suggests that there might be an underlying setting manageable through an API or 
potentially the gcloud CLI. 

Further exploration reveals a Stack Overflow thread where a user specifically inquired 
about enabling Firebase Auth via email through an API or CLI.12 The answer indicates 
that as of November 2022, this is achievable using Terraform or the gcloud CLI in 
conjunction with scripting. The solution involving gcloud suggests obtaining a service 
account access token and then using curl to send a PATCH request to the Identity 
Toolkit API. This approach is similar to what the user in the initial query attempted, but 
the key difference highlighted is the necessity of using a service account with the 
correct permissions. The thread also mentions the Terraform resource 
google_identity_platform_project_default_config, which directly addresses the 
configuration of default settings for the Identity Platform project, including enabling 
sign-in methods. This resource appears to be a strong candidate for programmatically 
performing the initial setup. While a dedicated gcloud firebase auth enable command 
is not evident, the possibility of using gcloud in conjunction with the Identity Platform 
API or a less obvious command related to it cannot be entirely dismissed. 

3.3. Firebase Identity Toolkit API 

The Firebase Identity Toolkit API is the RESTful interface that the user's script directly 
interacts with to configure authentication settings. The error encountered, 
"CONFIGURATION_NOT_FOUND," suggests that the /config endpoint expects Firebase 
Authentication to be in a specific initialized state before allowing modifications such 
as enabling email sign-in.13 Documentation for this API indicates that attempting to 
update the configuration of an identity provider that has not been previously 
configured for the project will result in an HTTP 404 error.13 This directly supports the 
idea that the email/password provider needs to be initially enabled before its specific 
settings can be modified. Community discussions also echo this, with users resolving 
similar errors by ensuring Firebase Authentication is enabled in the console.14 While 
the Identity Toolkit API allows for granular control over various authentication 
providers and settings, it appears to assume that the core Firebase Authentication 
service has already been initialized for the project. Therefore, the API call used by the 
user, while correct for configuring email sign-in, is likely failing because it is being 

made before the necessary initial setup has occurred. 

4. Analysis of Research Material 

The investigation of various programmatic options reveals a consistent theme: the 
"CONFIGURATION_NOT_FOUND" error signifies that Firebase Authentication requires 
an initial setup or enabling step before specific configurations, such as enabling email 
sign-in, can be applied via the Identity Toolkit API. The manual "Get started" button in 
the Firebase console appears to trigger this underlying process. While the Firebase 
Admin SDK provides extensive management capabilities, it lacks a direct method for 
this initial setup. The gcloud CLI presents a potential avenue, possibly through direct 
interaction with the Identity Platform API or via specific commands related to it. 
However, the most promising approach identified in the research is the use of the 
Terraform resource google_identity_platform_project_default_config. This resource is 
specifically designed to manage the default configuration of the Identity Platform 
project, which encompasses the enabling of various sign-in methods. The inability to 
automate this initial step using the user's current approach highlights a gap in the 
automated deployment of Firebase infrastructure within CI/CD pipelines. 

5. Proposed Solution and Implementation 

Based on the research, the most direct and effective programmatic method to 
replicate the initial setup of Firebase Authentication is by utilizing the 
google_identity_platform_project_default_config resource within Terraform.12 
Terraform is an infrastructure-as-code tool that allows for the declarative 
management of cloud resources and can be seamlessly integrated into CI/CD 
pipelines. 

To implement this solution, the following steps should be taken: 

1.  Install Terraform: Ensure that Terraform is installed in the CI/CD environment 
where the pipeline will execute. Installation instructions for various operating 
systems can be found on the official Terraform website. 

2.  Configure Google Cloud Provider: Configure the Google Cloud provider within 
Terraform. This typically involves creating a service account in Google Cloud with 
the necessary permissions and providing the credentials to Terraform. The 
service account will likely require the roles/identityplatform.admin IAM role to 
manage the Identity Platform configuration. 

3.  Create Terraform Configuration File: Create a Terraform configuration file (e.g., 

main.tf) that defines the google_identity_platform_project_default_config 
resource. The following is an example configuration that enables email sign-in: 

Terraform 
resource "google_identity_platform_project_default_config" "default" { 
  project = "<YOUR_FIREBASE_PROJECT_ID>" 
  sign_in { 
    email { 
      enabled = true 
    } 
    anonymous { 
      enabled = false 
    } 
    phone_number { 
      enabled = false 
    } 
  } 
} 

Replace <YOUR_FIREBASE_PROJECT_ID> with the actual ID of your Firebase 
project. This configuration explicitly sets the enabled property for email sign-in to 
true, effectively performing the initial enabling of this authentication method. The 
anonymous and phone_number blocks are included for completeness and can be 
adjusted as needed. 

4.  Apply Terraform Configuration: Integrate the Terraform commands into your 
Google Cloud Build pipeline. This would typically involve the following steps: 
○  terraform init: Initializes the Terraform working directory and downloads the 

necessary provider plugins. 

○  terraform plan: Creates an execution plan, showing what actions Terraform 

will take to achieve the desired state. This is useful for verifying the 
configuration before applying it. 

○  terraform apply --auto-approve: Applies the changes defined in the 

configuration file. The --auto-approve flag is used for non-interactive 
execution in a CI/CD environment. 

By incorporating these Terraform steps into your CI/CD pipeline before the curl 
command that attempts to further configure email authentication settings, the initial 
setup of Firebase Authentication will be performed programmatically. 

6. Alternative Approaches and Considerations 

While the Terraform solution appears to be the most direct, alternative approaches 
and important considerations should be discussed. Further investigation into the 

 
gcloud identity-platform command group might reveal a specific command capable of 
performing the initial setup. However, the research did not yield a readily apparent 
command for this purpose. 

The permissions granted to the service account used in the CI/CD pipeline are critical. 
As mentioned earlier, the roles/identityplatform.admin role is likely necessary for 
Terraform to modify the Identity Platform configuration. If an alternative approach 
using the Identity Toolkit API directly with gcloud auth print-access-token is pursued, 
ensuring the service account has the "Identity Toolkit Admin" role (as suggested in 6) 
will be crucial. Experimentation with different initial payloads for the PATCH request to 
the /config endpoint using a service account token might also be necessary to trigger 
the initial setup. 

Idempotency is a key principle in infrastructure automation, ensuring that applying the 
same configuration multiple times has the same effect as applying it once. Terraform 
inherently handles idempotency by comparing the current state of the infrastructure 
with the desired state defined in the configuration and only making necessary 
changes. 

It is also important to acknowledge that cloud APIs and services can evolve. 
Therefore, staying updated with the latest Firebase and Google Cloud documentation 
is essential to ensure the continued effectiveness of the chosen automation method. 

7. Conclusion 

The "CONFIGURATION_NOT_FOUND" error encountered when attempting to enable 
Firebase Authentication programmatically in a Google Cloud CI/CD pipeline indicates 
that an initial setup step, similar to the manual “Get started” action in the Firebase 
console, is required. Based on the research conducted, the most straightforward and 
reliable programmatic solution is to utilize the 
google_identity_platform_project_default_config resource within Terraform. By 
integrating Terraform into the CI/CD pipeline to apply a configuration that explicitly 
enables email sign-in, the initial setup can be automated. While alternative 
approaches using the gcloud CLI or direct interaction with the Identity Toolkit API 
might be possible, the Terraform method offers a declarative and well-established 
way to manage this aspect of Firebase Authentication. It is recommended that the 
user implement the proposed Terraform solution in their CI/CD pipeline, ensuring the 
service account used has the necessary permissions to modify the Identity Platform 
configuration. Further exploration of gcloud identity-platform commands or 
experimentation with the Identity Toolkit API using a service account token could be 

considered as alternative strategies. 

Table 1: Firebase Authentication Initialization Methods 

Method 

Description 

Ability to Initialize 

Snippet References 

Firebase Admin SDK 

Libraries for 
server-side 
interaction with 
Firebase services. 

No 

3-5-6-5-4 

Google Cloud CLI 
(gcloud) 

Command-line 
interface for Google 
Cloud services. 

Potentially 

10 

Firebase Identity 
Toolkit API 

Firebase Console 
(Manual) 

No 

Yes 

REST API for 
managing Firebase 
Authentication. 

Graphical user 
interface for 
managing Firebase 
projects. 

13-2-13 

1 

Table 2: Required IAM Roles for Programmatic Initialization 

Method 

Required IAM Role(s) 

Snippet References 

Terraform 

roles/identityplatform.admin 

gcloud CLI (Potential) 

roles/identityplatform.admin, 
"Identity Toolkit Admin" 

Identity Toolkit API (Potential) 

"Identity Toolkit Admin" 

12 

6 

6 

Works cited 

1.  Google Identity Toolkit returns CONFIGURATION_NOT_FOUND - Stack Overflow, 

accessed on April 7, 2025, 
https://stackoverflow.com/questions/32924049/google-identity-toolkit-returns-c

 
 
onfiguration-not-found 

2.  Identity Toolkit API config returns "CONFIGURATION_NOT_FOUND" - Stack 

Overflow, accessed on April 7, 2025, 
https://stackoverflow.com/questions/73344220/identity-toolkit-api-config-return
s-configuration-not-found 

3.  Authentication — Firebase Admin SDK for PHP Documentation, accessed on April 

7, 2025, https://firebase-php.readthedocs.io/en/7.10.0/authentication.html 

4.  Introduction to the Admin Auth API | Firebase Authentication - Google, accessed 

on April 7, 2025, https://firebase.google.com/docs/auth/admin 

5.  Introduction to the Admin Auth API | Identity Platform Documentation - Google 

Cloud, accessed on April 7, 2025, 
https://cloud.google.com/identity-platform/docs/concepts-admin-auth-api 
6.  Installing the Admin SDK | Identity Platform Documentation - Google Cloud, 

accessed on April 7, 2025, 
https://cloud.google.com/identity-platform/docs/install-admin-sdk 

7.  Add the Firebase Admin SDK to your server - Google, accessed on April 7, 2025, 

https://firebase.google.com/docs/admin/setup 

8.  Re: How to authenticate firebase-admin with nodejs... - Google Cloud 

Community, accessed on April 7, 2025, 
https://www.googlecloudcommunity.com/gc/Databases/How-to-authenticate-fir
ebase-admin-with-nodejs/m-p/845492 

9.  Manage Users | Firebase Authentication - Google, accessed on April 7, 2025, 

https://firebase.google.com/docs/auth/admin/manage-users 

10. gcloud firebase - Fig.io, accessed on April 7, 2025, 

https://fig.io/manual/gcloud/firebase 

11. Google Cloud Services - Firebase Admin - Quarkiverse Documentation, accessed 

on April 7, 2025, 
https://docs.quarkiverse.io/quarkus-google-cloud-services/main/firebase-admin.
html 

12. Is there a way to enable Firebase Auth via Email through an API/CLI? - Stack 

Overflow, accessed on April 7, 2025, 
https://stackoverflow.com/questions/64485179/is-there-a-way-to-enable-firebas
e-auth-via-email-through-an-api-cli 

13. Programmatically configure OAuth identity providers for Firebase Authentication, 

accessed on April 7, 2025, 
https://firebase.google.com/docs/auth/configure-oauth-rest-api 

14. What does Firebase - auth/configuration-not-found error mean ? - FlutterFlow 

Community, accessed on April 7, 2025, 
https://community.flutterflow.io/troubleshooting/post/what-does-firebase---auth
-configuration-not-found-error-mean-DyJvUD35A4h7Sti 

15. Using Firebase to authenticate users | API Gateway Documentation - Google 

Cloud, accessed on April 7, 2025, 
https://cloud.google.com/api-gateway/docs/authenticating-users-firebase 

16. End user authentication for Cloud Run tutorial, accessed on April 7, 2025, 

https://cloud.google.com/run/docs/tutorials/identity-platform 

17. Firebase Auth: How To Use Its REST API In 10 Minutes (2022) - Rowy, accessed on 

April 7, 2025, https://www.rowy.io/blog/firebase-auth-rest-api 

18. How to createUser using Firebase REST API? - Stack Overflow, accessed on April 

7, 2025, 
https://stackoverflow.com/questions/36027535/how-to-createuser-using-firebas
e-rest-api 

