
const API_URL = "http://localhost:8080";

let availableProperties = [];

async function initializeBuyerDashboard() {
    try {
        // Verify the signed-in user using the server session.
        const sessionResponse = await fetch(
            `${API_URL}/api/auth/me`,
            { credentials: "include" }
        );

        if (!sessionResponse.ok) {
            window.location.replace("login.html");
            return;
        }

        const user = await sessionResponse.json();

        if (user.role !== "BUYER") {
            window.location.replace("login.html");
            return;
        }

        document.getElementById("welcomeUser").textContent =
            `Welcome, ${user.name}`;

        await loadProperties();
        await loadMyBookings();
        await loadBuyerTransactionHistory();

    } catch (error) {
        console.error(error);
        document.getElementById("propertyList").textContent =
            "Unable to connect to the server.";
    }
}

async function loadProperties() {
    const response = await fetch(
        `${API_URL}/api/properties`,
        { credentials: "include" }
    );

    if (!response.ok) {
        throw new Error("Failed to load properties");
    }

    const properties = await response.json();

    availableProperties = properties.filter(
        property => property.status === "AVAILABLE"
    );

    displayProperties(availableProperties);
}

function displayProperties(properties) {
    const container = document.getElementById("propertyList");
    container.replaceChildren();

    document.getElementById("resultCount").textContent =
        `${properties.length} properties found`;

    if (properties.length === 0) {
        container.textContent =
            "No available properties match your search.";
        return;
    }

    properties.forEach(property => {
        const card = document.createElement("div");
        card.className = "property-card";

        const image = document.createElement("img");
        
image.src = property.image
    ? new URL(property.image, "http://localhost:8080").href
    : "http://localhost:8080/images/apartment.jpg";

        image.alt = property.title;

        const info = document.createElement("div");
        info.className = "property-info";

        const title = document.createElement("h3");
        title.textContent = property.title;

        const city = document.createElement("p");
        city.textContent = `Location: ${property.city}`;

        const type = document.createElement("p");
        type.textContent =
            `Type: ${property.type === "SALE" ? "For Sale" : "For Rent"}`;

        const bedrooms = document.createElement("p");
        bedrooms.textContent =
            `Bedrooms: ${property.bedrooms ?? "N/A"}`;

        const price = document.createElement("p");
        price.className = "price";

        price.textContent =
            "₹" + Number(property.price).toLocaleString("en-IN") +
            (property.type === "RENT" ? " / month" : "");

        const button = document.createElement("button");
        button.className = "details-btn";
        button.textContent = "View Details";

        
button.addEventListener("click", () => {
    openPropertyDetails(property);
});


        info.append(title, city, type, bedrooms, price, button);
        card.append(image, info);
        container.appendChild(card);
    });
}

function filterProperties() {
    const city =
        document.getElementById("cityFilter").value
            .trim().toLowerCase();

    const type =
        document.getElementById("typeFilter").value;

    const minPrice =
        document.getElementById("minPrice").value;

    const maxPrice =
        document.getElementById("maxPrice").value;

    const bedrooms =
        document.getElementById("bedroomFilter").value;

    const filtered = availableProperties.filter(property => {
        const matchesCity =
            !city ||
            property.city?.toLowerCase().includes(city);

        const matchesType =
            !type || property.type === type;

        const matchesMin =
            minPrice === "" ||
            Number(property.price) >= Number(minPrice);

        const matchesMax =
            maxPrice === "" ||
            Number(property.price) <= Number(maxPrice);

        const matchesBedrooms =
            !bedrooms ||
            Number(property.bedrooms) >= Number(bedrooms);

        return matchesCity &&
               matchesType &&
               matchesMin &&
               matchesMax &&
               matchesBedrooms;
    });

    displayProperties(filtered);
}

document.getElementById("searchBtn")
    .addEventListener("click", filterProperties);

document.getElementById("resetBtn")
    .addEventListener("click", () => {
        document.getElementById("cityFilter").value = "";
        document.getElementById("typeFilter").value = "";
        document.getElementById("minPrice").value = "";
        document.getElementById("maxPrice").value = "";
        document.getElementById("bedroomFilter").value = "";

        displayProperties(availableProperties);
    });

document.getElementById("logoutBtn")
    .addEventListener("click", async () => {
        try {
            await fetch(`${API_URL}/api/auth/logout`, {
                method: "POST",
                credentials: "include"
            });
            window.location.replace("login.html");
        } catch (error) {
            alert("Unable to log out. Please try again.");
        }
    });

// ======================================
// PROPERTY DETAILS POPUP
// ======================================

const propertyModal =
    document.getElementById("propertyModal");

const closePropertyModal =
    document.getElementById("closePropertyModal");

let previousFocus = null;
let selectedPropertyId = null;
function openPropertyDetails(property) {
    selectedPropertyId = property.id;
    bookPropertyBtn.disabled = false;
bookingStatus.textContent = "";
    // Reset visit form for the selected property
document.getElementById("visitForm").reset();
document.getElementById("visitForm").hidden = true;
document.getElementById("bookVisitBtn").hidden = false;
document.getElementById("visitStatus").textContent = "";

document.getElementById("enquiryForm").hidden = true;
document.getElementById("enquiryForm").reset();
document.getElementById("enquiryStatus").textContent = "";
document.getElementById("enquireBtn").hidden = false;
    
document.getElementById("modalSellerName").textContent =
    property.sellerName || "Property Owner";

    previousFocus = document.activeElement;

    document.getElementById("modalTitle").textContent =
        property.title;

    document.getElementById("modalPrice").textContent =
        `₹${Number(property.price).toLocaleString("en-IN")}`;

    document.getElementById("modalLocation").textContent =
        `${property.city} · ${property.type === "RENT" ? "For Rent" : "For Sale"}`;

    document.getElementById("modalBedrooms").textContent =
        `${property.bedrooms ?? "N/A"} Bedrooms`;

    document.getElementById("modalBathrooms").textContent =
        `${property.bathrooms ?? "N/A"} Bathrooms`;

    document.getElementById("modalArea").textContent =
        `${property.area ?? "N/A"} sq.ft.`;

    document.getElementById("modalDescription").textContent =
        property.description || "No description available.";

    document.getElementById("modalAddress").textContent =
        property.address || "Address not available.";

    const image = document.getElementById("modalImage");

    loadPropertyGallery(property.id, property.image);

    propertyModal.hidden = false;
    document.body.style.overflow = "hidden";
    closePropertyModal.focus();
}

function hidePropertyDetails() {
    propertyModal.hidden = true;
    document.body.style.overflow = "";

    if (previousFocus && previousFocus.isConnected) {
        previousFocus.focus();
    }
}

closePropertyModal.addEventListener(
    "click",
    hidePropertyDetails
);

propertyModal.addEventListener("click", event => {
    if (event.target === propertyModal) {
        hidePropertyDetails();
    }
});

document.addEventListener("keydown", event => {
    if (propertyModal.hidden) return;

    if (event.key === "Escape") {
        hidePropertyDetails();
    }

    if (event.key === "Tab") {
        event.preventDefault();
        closePropertyModal.focus();
    }
});


// ======================================
// MULTIPLE PROPERTY IMAGE GALLERY
// ======================================

let galleryImages = [];
let currentImageIndex = 0;
let galleryRequestId = 0;

const prevImageBtn = document.getElementById("prevImage");
const nextImageBtn = document.getElementById("nextImage");

function getImageUrl(path) {
    return new URL(
        path || "/images/apartment.jpg",
        "http://localhost:8080"
    ).href;
}

function showGalleryImage(index) {
    if (galleryImages.length === 0) return;

    currentImageIndex = index;

    const mainImage = document.getElementById("modalImage");
    const counter = document.getElementById("imageCounter");
    const thumbnails =
        document.getElementById("galleryThumbnails");

    mainImage.src = galleryImages[index];
    mainImage.alt =
        `Property image ${index + 1} of ${galleryImages.length}`;

    counter.textContent =
        `${index + 1} / ${galleryImages.length} Photos`;

    const hasMultiple = galleryImages.length > 1;

    prevImageBtn.disabled = !hasMultiple;
    nextImageBtn.disabled = !hasMultiple;

    Array.from(thumbnails.children).forEach((thumbnail, i) => {
        thumbnail.classList.toggle("active", i === index);
        thumbnail.setAttribute(
            "aria-pressed", String(i === index)
        );
    });
}

async function loadPropertyGallery(propertyId, fallbackImage) {
    const requestId = ++galleryRequestId;

    galleryImages = [getImageUrl(fallbackImage)];
    currentImageIndex = 0;

    const thumbnails =
        document.getElementById("galleryThumbnails");

    thumbnails.replaceChildren();
    showGalleryImage(0);

    try {
        const response = await fetch(
            `${API_URL}/api/properties/${propertyId}/images`,
            { credentials: "include" }
        );

        if (!response.ok) {
            throw new Error("Unable to load property images");
        }

        const images = await response.json();

        // Ignore an outdated response if another property was opened.
        if (requestId !== galleryRequestId) return;

        if (images.length > 0) {
            galleryImages = images.map(image =>
                getImageUrl(image.imageUrl)
            );
        }

        thumbnails.replaceChildren();

        galleryImages.forEach((url, index) => {
            const thumbnail = document.createElement("img");

            thumbnail.src = url;
            thumbnail.alt = `View photo ${index + 1}`;
            thumbnail.className = "gallery-thumbnail";
            thumbnail.setAttribute("role", "button");
            thumbnail.setAttribute("tabindex", "0");

            const selectImage = () => showGalleryImage(index);

            thumbnail.addEventListener("click", selectImage);
            thumbnail.addEventListener("keydown", event => {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    selectImage();
                }
            });

            thumbnails.appendChild(thumbnail);
        });

        showGalleryImage(0);

    } catch (error) {
        if (requestId !== galleryRequestId) return;
        console.error("Gallery error:", error);
        // Keep displaying the main property image.
    }
}

prevImageBtn.addEventListener("click", () => {
    const index =
        (currentImageIndex - 1 + galleryImages.length)
        % galleryImages.length;

    showGalleryImage(index);
});

nextImageBtn.addEventListener("click", () => {
    const index =
        (currentImageIndex + 1) % galleryImages.length;

    showGalleryImage(index);
});


/* ===================================
   BUYER ENQUIRY FORM
=================================== */

const enquireBtn = document.getElementById("enquireBtn");
const enquiryForm = document.getElementById("enquiryForm");
const enquiryStatus = document.getElementById("enquiryStatus");
const sendEnquiryBtn = document.getElementById("sendEnquiryBtn");

// Show enquiry form when button is clicked
enquireBtn.addEventListener("click", () => {
    enquiryForm.hidden = false;
    enquireBtn.hidden = true;

    document.getElementById("enquiryMessage").focus();
});

// Submit enquiry to Spring Boot
enquiryForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const message = document
        .getElementById("enquiryMessage")
        .value.trim();

    if (selectedPropertyId === null || !message) {
        enquiryStatus.textContent =
            "Please select a property and enter a message.";
        return;
    }

    const propertyId = selectedPropertyId;

    sendEnquiryBtn.disabled = true;
    enquiryStatus.textContent = "Sending enquiry...";

    try {
        // Get CSRF token
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Please log in again.");
        }

        const { csrfToken } = await csrfResponse.json();

        // Submit enquiry
        const response = await fetch(
            "http://localhost:8080/api/enquiries",
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    propertyId: propertyId,
                    message: message
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Failed to submit enquiry."
            );
        }

        enquiryStatus.textContent =
            "Enquiry submitted successfully!";

        document.getElementById("enquiryMessage").value = "";

    } catch (error) {
        console.error("Enquiry error:", error);
        enquiryStatus.textContent = error.message;

    } finally {
        sendEnquiryBtn.disabled = false;
    }
});


/* =====================================
   BOOK PROPERTY VISIT
===================================== */

const bookVisitBtn = document.getElementById("bookVisitBtn");
const visitForm = document.getElementById("visitForm");
const visitStatus = document.getElementById("visitStatus");
const submitVisitBtn = document.getElementById("submitVisitBtn");
const visitDateInput = document.getElementById("visitDate");

// Get today's date using the browser's local date
function getTodayDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

// Show the visit booking form
bookVisitBtn.addEventListener("click", () => {
    visitForm.hidden = false;
    bookVisitBtn.hidden = true;

    visitDateInput.min = getTodayDate();
    visitDateInput.focus();
});

// Submit visit booking
visitForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (selectedPropertyId === null) {
        visitStatus.textContent = "Please select a property.";
        return;
    }

    const propertyId = selectedPropertyId;
    const visitDate = visitDateInput.value;
    const visitTime = document.getElementById("visitTime").value;
    const remarks = document.getElementById("visitRemarks").value.trim();

    if (!visitDate || !visitTime) {
        visitStatus.textContent =
            "Please select a visit date and time.";
        return;
    }

    if (new Date(`${visitDate}T${visitTime}`) <= new Date()) {
        visitStatus.textContent =
            "Please select a future date and time.";
        return;
    }

    submitVisitBtn.disabled = true;
    visitStatus.textContent = "Booking your visit...";

    try {
        // Retrieve CSRF token
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Please log in again.");
        }

        const { csrfToken } = await csrfResponse.json();

        // Submit property visit
        const response = await fetch(
            "http://localhost:8080/api/visits",
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    propertyId,
                    visitDate,
                    visitTime,
                    remarks
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Unable to book visit."
            );
        }

        visitStatus.textContent =
            "Property visit booked successfully!";

        visitForm.reset();

    } catch (error) {
        console.error("Visit booking error:", error);
        visitStatus.textContent = error.message;

    } finally {
        submitVisitBtn.disabled = false;
    }
});


/* PROPERTY BOOKING */

const bookPropertyBtn =
    document.getElementById("bookPropertyBtn");

const bookingStatus =
    document.getElementById("bookingStatus");

bookPropertyBtn.addEventListener("click", async () => {

    if (selectedPropertyId == null) {
        bookingStatus.textContent =
            "Please select a property.";
        return;
    }

    const confirmed = confirm(
        "Do you want to submit a booking request for this property?"
    );

    if (!confirmed) return;

    bookPropertyBtn.disabled = true;
    bookingStatus.textContent = "Submitting booking...";

    try {
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Please log in again.");
        }

        const { csrfToken } = await csrfResponse.json();

        const response = await fetch(
            "http://localhost:8080/api/bookings",
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    propertyId: selectedPropertyId
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Booking failed."
            );
        }

        bookingStatus.textContent =
            "Booking request submitted successfully!";

    } catch (error) {
        console.error("Booking error:", error);
        bookingStatus.textContent = error.message;
        bookPropertyBtn.disabled = false;
    }
});

/* =====================================
   LOAD BUYER BOOKINGS
===================================== */

/* COMPLETE DEMO PAYMENT */

async function completeDemoPayment(paymentId, button) {

    if (!confirm(
        "Mark this simulated payment as successful? No real money will be transferred."
    )) {
        return;
    }

    button.disabled = true;
    button.textContent = "Completing...";

    try {
        // Get CSRF token
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Please log in again.");
        }

        const { csrfToken } = await csrfResponse.json();

        // Complete simulated payment
        const response = await fetch(
            `http://localhost:8080/api/payments/${paymentId}/complete`,
            {
                method: "PUT",
                credentials: "include",
                headers: {
                    "X-CSRF-Token": csrfToken
                }
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Payment completion failed."
            );
        }

        alert("Demo payment marked successful!");

        // Refresh bookings
        await loadMyBookings();

    } catch (error) {
        console.error("Payment completion error:", error);
        alert(error.message);

    } finally {
        button.disabled = false;
        button.textContent = "Complete Demo Payment";
    }
}

/* =====================================
   BUYER TRANSACTION HISTORY
===================================== */

async function loadBuyerTransactionHistory() {

    const salesList =
        document.getElementById("buyerSalesList");

    const rentalsList =
        document.getElementById("buyerRentalsList");

    if (!salesList || !rentalsList) return;

    salesList.textContent = "Loading purchases...";
    rentalsList.textContent = "Loading rental agreements...";

    // Format amounts as Indian Rupees
    const formatCurrency = (amount) => {
        return Number(amount).toLocaleString("en-IN", {
            style: "currency",
            currency: "INR"
        });
    };

    // Create a safe text row
    const addDetail = (card, label, value) => {
        const paragraph = document.createElement("p");

        const strong = document.createElement("strong");
        strong.textContent = label + ": ";

        paragraph.appendChild(strong);
        paragraph.appendChild(
            document.createTextNode(String(value ?? "—"))
        );

        card.appendChild(paragraph);
    };

    try {

        const response = await fetch(
            "http://localhost:8080/api/transactions/my",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Unable to load transaction history.");
        }

        const data = await response.json();

        salesList.replaceChildren();
        rentalsList.replaceChildren();

        // ============================
        // PROPERTY PURCHASES
        // ============================

        if (!data.sales || data.sales.length === 0) {

            salesList.textContent =
                "No property purchases found.";

        } else {

            data.sales.forEach((sale) => {

                const card = document.createElement("div");
                card.className = "transaction-card";

                const heading = document.createElement("h4");
                heading.textContent = sale.propertyTitle;
                card.appendChild(heading);

                addDetail(card, "Sale ID", sale.transactionId);
                addDetail(card, "Booking ID", sale.bookingId);
                addDetail(card, "Transaction Type", "SALE");
                addDetail(card, "Sale Date", sale.transactionDate);
                addDetail(
                    card,
                    "Final Price",
                    formatCurrency(sale.amount)
                );
                addDetail(
                    card,
                    "Registration Reference",
                    sale.referenceNumber
                );
                addDetail(card, "Sale Status", sale.status);

                salesList.appendChild(card);
            });
        }

        // ============================
        // RENTAL AGREEMENTS
        // ============================

        if (!data.rentals || data.rentals.length === 0) {

            rentalsList.textContent =
                "No rental agreements found.";

        } else {

            data.rentals.forEach((rental) => {

                const card = document.createElement("div");
                card.className = "transaction-card";

                const heading = document.createElement("h4");
                heading.textContent = rental.propertyTitle;
                card.appendChild(heading);

                addDetail(
                    card,
                    "Agreement ID",
                    rental.transactionId
                );
                addDetail(card, "Booking ID", rental.bookingId);
                addDetail(card, "Transaction Type", "RENT");
                addDetail(card, "Start Date", rental.startDate);
                addDetail(card, "End Date", rental.endDate);
                addDetail(
                    card,
                    "Monthly Rent",
                    formatCurrency(rental.monthlyRent)
                );
                addDetail(
                    card,
                    "Security Deposit",
                    formatCurrency(rental.securityDeposit)
                );
                addDetail(
                    card,
                    "Agreement Status",
                    rental.status
                );

                rentalsList.appendChild(card);
            });
        }

    } catch (error) {

        console.error("Transaction history error:", error);

        salesList.textContent =
            "Unable to load property purchases.";

        rentalsList.textContent =
            "Unable to load rental agreements.";
    }
}

async function loadMyBookings() {

    const bookingsList =
        document.getElementById("myBookingsList");

    if (!bookingsList) return;

    bookingsList.textContent = "Loading your bookings...";

    try {
        const response = await fetch(
            "http://localhost:8080/api/bookings/my",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load bookings.");
        }

        const bookings = await response.json();

        bookingsList.replaceChildren();

        if (bookings.length === 0) {
            bookingsList.textContent =
                "You have not booked any properties yet.";
            return;
        }

        bookings.forEach((booking) => {

            const card = document.createElement("div");
            card.className = "buyer-booking-card";

            const heading = document.createElement("h3");
            heading.textContent = booking.propertyTitle;
            card.appendChild(heading);

            const amount = Number(booking.bookingAmount)
                .toLocaleString("en-IN", {
                    style: "currency",
                    currency: "INR"
                });

            const details = [
                ["Booking ID", booking.id],
                ["Booking Type", booking.bookingType],
                ["Booking Amount", amount],
                ["Booking Date", booking.bookingDate],
                ["Booking Status", booking.status],
                ["Payment Status",
                    booking.paymentStatus || "Not recorded"],
                ["Transaction Reference",
                    booking.transactionId || "Not available"]
            ];

            details.forEach(([label, value]) => {

                const paragraph = document.createElement("p");

                const strong = document.createElement("strong");
                strong.textContent = label + ": ";

                paragraph.appendChild(strong);
                paragraph.appendChild(
                    document.createTextNode(String(value ?? ""))
                );

                card.appendChild(paragraph);
            });

// Show payment option only for confirmed bookings
// without a pending or successful payment
if (
    booking.status === "CONFIRMED" &&
    !["PENDING", "SUCCESSFUL"].includes(booking.paymentStatus)
) {
    const paymentSection = document.createElement("div");
    paymentSection.className = "booking-payment-section";

    const label = document.createElement("label");
    label.textContent = "Select Payment Method";
    label.htmlFor = `paymentMethod-${booking.id}`;

    const paymentMethod = document.createElement("select");
    paymentMethod.id = `paymentMethod-${booking.id}`;

    const methods = [
        ["", "Choose payment method"],
        ["UPI", "UPI"],
        ["CARD", "Card"],
        ["BANK_TRANSFER", "Bank Transfer"],
        ["CASH", "Cash"]
    ];

    methods.forEach(([value, text]) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = text;
        paymentMethod.appendChild(option);
    });

    const paymentBtn = document.createElement("button");
    paymentBtn.type = "button";
    paymentBtn.className = "record-payment-btn";
    paymentBtn.textContent = "Record Demo Payment";

    const paymentMessage = document.createElement("p");
    paymentMessage.setAttribute("role", "status");

    paymentBtn.addEventListener("click", async () => {
        if (!paymentMethod.value) {
            paymentMessage.textContent =
                "Please select a payment method.";
            return;
        }

        if (!confirm(
            "Create a simulated payment record? No real money will be transferred."
        )) {
            return;
        }

        paymentBtn.disabled = true;
        paymentMessage.textContent = "Recording payment...";

        try {
            const csrfResponse = await fetch(
                "http://localhost:8080/api/auth/csrf",
                { credentials: "include" }
            );

            if (!csrfResponse.ok) {
                throw new Error("Please log in again.");
            }

            const { csrfToken } = await csrfResponse.json();

            const response = await fetch(
                "http://localhost:8080/api/payments",
                {
                    method: "POST",
                    credentials: "include",
                    headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-Token": csrfToken
                    },
                    body: JSON.stringify({
                        bookingId: booking.id,
                        paymentMethod: paymentMethod.value
                    })
                }
            );

            const result = await response.json();

            if (!response.ok) {
                throw new Error(
                    result.message || "Payment recording failed."
                );
            }

            paymentMessage.textContent =
                "Demo payment recorded successfully. Status: PENDING.";

            // Refresh bookings and payment status
            await loadMyBookings();
            

        } catch (error) {
            console.error("Payment recording error:", error);
            paymentMessage.textContent = error.message;
            paymentBtn.disabled = false;
        }
    });

    paymentSection.appendChild(label);
    paymentSection.appendChild(paymentMethod);
    paymentSection.appendChild(paymentBtn);
    paymentSection.appendChild(paymentMessage);

    card.appendChild(paymentSection);
}


// Complete demo payment button
if (
    booking.status === "CONFIRMED" &&
    booking.paymentStatus === "PENDING" &&
    booking.paymentId != null
) {
    const completeBtn = document.createElement("button");

    completeBtn.type = "button";
    completeBtn.className = "complete-payment-btn";
    completeBtn.textContent = "Complete Demo Payment";

    completeBtn.addEventListener("click", () => {
        completeDemoPayment(booking.paymentId, completeBtn);
    });

    card.appendChild(completeBtn);
}

            bookingsList.appendChild(card);
        });

    } catch (error) {
        console.error("My bookings error:", error);
        bookingsList.textContent =
            "Unable to load your bookings.";
    }
}

initializeBuyerDashboard();