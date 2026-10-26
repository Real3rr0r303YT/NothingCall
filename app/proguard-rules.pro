# NothingCall – R8 rules.
# Components declared in the manifest (InCallService, services, receivers) are kept automatically.
# No reflection-based libraries are used, so no extra keep rules are required.
-dontwarn org.jetbrains.annotations.**
