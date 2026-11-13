
"use strict";

const loginForm = document.getElementById("loginForm");
const signupForm = document.getElementById("signupForm");

// Sign In

if (loginForm) {
    loginForm.addEventListener("submit", async event => {
        event.preventDefault();

        const message =
            document.getElementById("loginMessage");

        const email =
            document.getElementById("loginEmail").value.trim();

        const password =
            document.getElementById("loginPassword").value;

        message.textContent = "Verifying credentials...";

        try {
            const response = await fetch(
                "http://localhost:8080/api/auth/login",
                {
                    method: "POST",
                    credentials: "include",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        email,
                        password
                    })
                }
            );

            const result = await response.json();

            if (!response.ok) {
                message.textContent =
                    result.message || "Login failed";
                return;
            }

           
message.textContent =
    "Welcome, " + result.name + "! Redirecting...";

if (result.role === "BUYER") {
    window.location.href = "buyer.html";
} else if (result.role === "SELLER") {
    window.location.href = "seller.html";
} else {
    message.textContent = "Unsupported account role.";
}


            // Dashboard navigation will be added
            // after server-side sessions are implemented.

        } catch (error) {
            console.error("Login error:", error);

            message.textContent =
                "Unable to connect to the server.";
        }
    });
}


// Sign Up

if (signupForm) {
    signupForm.addEventListener("submit", async event => {
        event.preventDefault();

        const message =
            document.getElementById("signupMessage");

        const user = {
            name: document.getElementById("signupName").value.trim(),
            email: document.getElementById("signupEmail").value.trim(),
            phone: document.getElementById("signupPhone").value.trim(),
            password: document.getElementById("signupPassword").value,
            role: document.getElementById("signupRole").value
        };

        message.textContent = "Creating account...";

        try {
            const response = await fetch(
                "http://localhost:8080/api/auth/signup",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(user)
                }
            );

            const result = await response.json();

            if (!response.ok) {
                message.textContent =
                    result.message || "Registration failed";
                return;
            }

            message.textContent =
                "Account created! Redirecting to Sign In...";

            signupForm.reset();

            setTimeout(() => {
                window.location.href = "login.html";
            }, 1500);

        } catch (error) {
            console.error("Signup error:", error);
            message.textContent =
                "Unable to connect to the server.";
        }
    });
}

