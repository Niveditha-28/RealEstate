
const API_URL = "http://localhost:8080";

const propertyForm = document.getElementById("propertyForm");
const formMessage = document.getElementById("formMessage");
const submitBtn = document.getElementById("submitBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");

let editingPropertyId = null;

// ======================================
// 1. VERIFY SELLER LOGIN
// ======================================

async function initializeSellerDashboard() {
    try {
        const response = await fetch(
            `${API_URL}/api/auth/me`,
            { credentials: "include" }
        );

        if (!response.ok) {
            window.location.href = "login.html";
            return;
        }

        const user = await response.json();

        if (user.role !== "SELLER") {
            window.location.href = "login.html";
            return;
        }

        const welcomeSeller =
            document.getElementById("welcomeSeller");

        if (welcomeSeller) {
            welcomeSeller.textContent =
                `Welcome, ${user.name}`;
        }

        await loadPropertyTypes();
        await loadMyProperties();
        await loadSellerEnquiries();
        await loadSellerVisits();
        await loadSellerBookings();
        await loadSellerTransactionHistory();

    } catch (error) {
        console.error("Login verification error:", error);
        window.location.href = "login.html";
    }
}

// ======================================
// 2. LOAD PROPERTY TYPES FROM MYSQL
// ======================================

async function loadPropertyTypes() {
    const select = document.getElementById("propertyType");

    select.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Select Property Type";
    select.appendChild(placeholder);

    try {
        const response = await fetch(
            `${API_URL}/api/property-types`,
            { credentials: "include" }
        );

        if (!response.ok) {
            throw new Error("Unable to load property types");
        }

        const types = await response.json();

        console.log("Property types received:", types);

        types.forEach(type => {
            const option = document.createElement("option");

            option.value = type.id;
            option.textContent = type.name;

            select.appendChild(option);
        });

    } catch (error) {
        console.error("Property type error:", error);
        formMessage.textContent = error.message;
    }
}

// ======================================
// 3. LOAD SELLER'S PROPERTIES
// ======================================

async function loadMyProperties() {
    const container = document.getElementById("myProperties");

    if (!container) return;

    container.textContent = "Loading your properties...";

    try {
        const response = await fetch(
            `${API_URL}/api/seller/properties`,
            { credentials: "include" }
        );

        if (!response.ok) {
            throw new Error("Unable to load your properties");
        }

        const properties = await response.json();

        displayMyProperties(properties);

    } catch (error) {
        console.error("My Properties error:", error);
        container.textContent = error.message;
    }
}

// ======================================
// 4. DISPLAY SELLER PROPERTIES
// ======================================

function displayMyProperties(properties) {
    const container = document.getElementById("myProperties");

    container.replaceChildren();

    if (properties.length === 0) {
        container.textContent =
            "You haven't listed any properties yet.";
        return;
    }

    properties.forEach(property => {
        const card = document.createElement("div");
        card.className = "seller-property-card";

        const image = document.createElement("img");

        image.src = property.image
            ? new URL(property.image, API_URL).href
            : `${API_URL}/images/apartment.jpg`;

        image.alt = property.title;

        const info = document.createElement("div");

        const title = document.createElement("h3");
        title.textContent = property.title;

        const city = document.createElement("p");
        city.textContent =
            `Location: ${property.city}`;

        const type = document.createElement("p");
        type.textContent =
            `Property Type: ${property.propertyType}`;

        const listing = document.createElement("p");
        listing.textContent =
            `Listing: ${property.type}`;

        const price = document.createElement("p");
        price.textContent =
            `Price: ₹${Number(property.price).toLocaleString("en-IN")}`;

        const status = document.createElement("p");
        status.textContent =
            `Status: ${property.status}`;

        // EDIT BUTTON
        const editButton = document.createElement("button");

        editButton.textContent = "Edit Property";
        editButton.className = "edit-btn";

        editButton.disabled =
            property.status !== "AVAILABLE";

        editButton.addEventListener("click", () => {
            startEditingProperty(property);
        });

        info.append(
            title,
            city,
            type,
            listing,
            price,
            status,
            editButton
        );

        card.append(image, info);
        container.appendChild(card);
    });
}

// ======================================
// 5. START EDITING A PROPERTY
// ======================================

function startEditingProperty(property) {
    editingPropertyId = property.id;

    document.getElementById("title").value =
        property.title ?? "";

    document.getElementById("propertyType").value =
        property.propertyTypeId;

    document.getElementById("listingType").value =
        property.type;

    document.getElementById("price").value =
        property.price;

    document.getElementById("city").value =
        property.city ?? "";

    document.getElementById("area").value =
        property.area ?? "";

    document.getElementById("bedrooms").value =
        property.bedrooms ?? 0;

    document.getElementById("bathrooms").value =
        property.bathrooms ?? 0;

    document.getElementById("address").value =
        property.address ?? "";

    document.getElementById("description").value =
        property.description ?? "";

    submitBtn.textContent = "Update Property";

    if (cancelEditBtn) {
        cancelEditBtn.hidden = false;
    }

    formMessage.textContent =
        `Editing property ID: ${property.id}`;

    propertyForm.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

// ======================================
// 6. CANCEL EDITING
// ======================================

function resetEditMode() {
    editingPropertyId = null;

    propertyForm.reset();
    document.getElementById("imagePreview").replaceChildren();

    submitBtn.textContent = "List Property";

    if (cancelEditBtn) {
        cancelEditBtn.hidden = true;
    }
}

if (cancelEditBtn) {
    cancelEditBtn.addEventListener("click", () => {
        resetEditMode();
        formMessage.textContent = "";
    });
}

// ======================================
// 7. ADD OR UPDATE PROPERTY
// ======================================

propertyForm.addEventListener("submit", async event => {
    event.preventDefault();

    const propertyTypeValue =
        document.getElementById("propertyType").value;

    if (!propertyTypeValue) {
        formMessage.textContent =
            "Please select a property type.";
        return;
    }

    const property = {
        title: document.getElementById("title").value.trim(),

        propertyTypeId: Number(propertyTypeValue),

        listingType:
            document.getElementById("listingType").value,

        price: Number(
            document.getElementById("price").value
        ),

        city: document.getElementById("city").value.trim(),

        area: Number(
            document.getElementById("area").value
        ),

        bedrooms: Number(
            document.getElementById("bedrooms").value
        ),

        bathrooms: Number(
            document.getElementById("bathrooms").value
        ),

        address:
            document.getElementById("address").value.trim(),

        description:
            document.getElementById("description").value.trim()
    };

    const isEditing = editingPropertyId !== null;
    const propertyIdBeingEdited = editingPropertyId;

    submitBtn.disabled = true;

    formMessage.textContent = isEditing
        ? "Updating property..."
        : "Saving property...";

    try {
        // GET CSRF TOKEN
        const csrfResponse = await fetch(
            `${API_URL}/api/auth/csrf`,
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Please sign in again");
        }

        const { csrfToken } = await csrfResponse.json();

        // CHOOSE POST OR PUT
        const url = isEditing
            ? `${API_URL}/api/seller/properties/${editingPropertyId}`
            : `${API_URL}/api/seller/properties`;

        const response = await fetch(url, {
            method: isEditing ? "PUT" : "POST",

            credentials: "include",

            headers: {
                "Content-Type": "application/json",
                "X-CSRF-Token": csrfToken
            },

            body: JSON.stringify(property)
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Unable to save property"
            );
        }

const savedPropertyId = isEditing
    ? propertyIdBeingEdited
    : result.propertyId;

const hasImages =
    document.getElementById("propertyImages").files.length > 0;

let uploadError = null;

if (hasImages) {
    formMessage.textContent = "Uploading property images...";

    try {
        await uploadPropertyImages(savedPropertyId);
    } catch (error) {
        uploadError = error;
        console.error("Image upload error:", error);
    }
}

resetEditMode();

document.getElementById("imagePreview").replaceChildren();

await loadMyProperties();

if (uploadError) {
    formMessage.textContent =
        "Property saved successfully, but images were not uploaded: " +
        uploadError.message;
} else {
    formMessage.textContent = isEditing
        ? "Property and selected images updated successfully!"
        : `Property listed successfully! ID: ${savedPropertyId}`;
}


    } catch (error) {
        console.error("Property save error:", error);
        formMessage.textContent = error.message;

    } finally {
        submitBtn.disabled = false;
    }
});

// ======================================
// 8. LOGOUT
// ======================================

const logoutBtn = document.getElementById("logoutBtn");

if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
        try {
            await fetch(`${API_URL}/api/auth/logout`, {
                method: "POST",
                credentials: "include"
            });
        } catch (error) {
            console.error("Logout error:", error);
        }

        window.location.href = "login.html";
    });
}

// ======================================
// 10. PROPERTY IMAGE PREVIEW
// ======================================

const propertyImagesInput =
    document.getElementById("propertyImages");

const imagePreview =
    document.getElementById("imagePreview");

propertyImagesInput.addEventListener("change", () => {
    imagePreview.replaceChildren();

    const files = Array.from(propertyImagesInput.files);

    if (files.length > 5) {
        alert("You can select a maximum of 5 images.");
        propertyImagesInput.value = "";
        return;
    }

    const allowedTypes = [
        "image/jpeg",
        "image/png",
        "image/webp"
    ];

    for (const file of files) {
        if (!allowedTypes.includes(file.type)) {
            alert("Only JPG, PNG and WEBP images are allowed.");
            propertyImagesInput.value = "";
            imagePreview.replaceChildren();
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            alert("Each image must be 5 MB or smaller.");
            propertyImagesInput.value = "";
            imagePreview.replaceChildren();
            return;
        }
    }

    files.forEach(file => {
        const image = document.createElement("img");
        const imageUrl = URL.createObjectURL(file);

        image.src = imageUrl;
        image.alt = file.name;

        image.addEventListener("load", () => {
            URL.revokeObjectURL(imageUrl);
        }, { once: true });

        imagePreview.appendChild(image);
    });
});

async function uploadPropertyImages(propertyId) {
    const imageInput = document.getElementById("propertyImages");
    const files = Array.from(imageInput.files);

    if (files.length === 0) {
        return;
    }

    const formData = new FormData();

    files.forEach(file => {
        formData.append("images", file);
    });

    // Retrieve CSRF token
    const csrfResponse = await fetch(
        `${API_URL}/api/auth/csrf`,
        { credentials: "include" }
    );

    if (!csrfResponse.ok) {
        throw new Error("Unable to verify upload session");
    }

    const { csrfToken } = await csrfResponse.json();

    // Upload images
    const response = await fetch(
        `${API_URL}/api/property-images/${propertyId}`,
        {
            method: "POST",
            credentials: "include",
            headers: {
                "X-CSRF-Token": csrfToken
            },
            body: formData
        }
    );

    const result = await response.json();

    if (!response.ok) {
        throw new Error(
            result.message || "Image upload failed"
        );
    }

    return result;
}

// =====================================
// SELLER ENQUIRIES
// =====================================

async function loadSellerEnquiries() {
    const list = document.getElementById("sellerEnquiriesList");
    const status = document.getElementById("sellerEnquiryStatus");

    if (!list || !status) return;

    status.textContent = "Loading enquiries...";
    list.replaceChildren();

    try {
        const response = await fetch(
            "http://localhost:8080/api/enquiries/seller",
            { credentials: "include" }
        );

        if (!response.ok) {
            throw new Error("Unable to load enquiries.");
        }

        const enquiries = await response.json();

        status.textContent = "";

        if (enquiries.length === 0) {
            status.textContent = "No enquiries received yet.";
            return;
        }

        enquiries.forEach(enquiry => {
            const card = document.createElement("div");
            card.className = "seller-enquiry-card";

            const top = document.createElement("div");
            top.className = "seller-enquiry-top";

            const title = document.createElement("h3");
            title.textContent = enquiry.propertyTitle;

            const badge = document.createElement("span");
            badge.className = "enquiry-status-badge";
            badge.textContent = enquiry.status;

            top.append(title, badge);

            const buyer = document.createElement("p");
            buyer.textContent = `Buyer: ${enquiry.buyerName}`;

            const message = document.createElement("p");
            message.className = "seller-enquiry-message";
            message.textContent = enquiry.message;

            const date = document.createElement("p");
            date.className = "enquiry-date";

            date.textContent = enquiry.enquiryDate
                ? `Received: ${new Date(
                    enquiry.enquiryDate
                ).toLocaleString("en-IN")}`
                : "Date unavailable";

            
card.append(top, buyer, message, date);

// Show reply form for OPEN enquiries
if (enquiry.status === "OPEN") {

    const replyForm = document.createElement("form");
    replyForm.className = "seller-reply-form";

    const replyInput = document.createElement("textarea");
    replyInput.className = "seller-reply-input";
    replyInput.placeholder = "Type your reply...";
    replyInput.maxLength = 1000;
    replyInput.required = true;
    replyInput.rows = 3;

    const replyButton = document.createElement("button");
    replyButton.type = "submit";
    replyButton.className = "seller-reply-btn";
    replyButton.textContent = "Send Reply";

    const replyStatus = document.createElement("p");
    replyStatus.className = "seller-reply-status";
    replyStatus.setAttribute("role", "status");

    replyForm.append(replyInput, replyButton, replyStatus);

    replyForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const reply = replyInput.value.trim();

        if (!reply || reply.length > 1000) {
            replyStatus.textContent =
                "Enter a reply between 1 and 1000 characters.";
            return;
        }

        replyButton.disabled = true;
        replyStatus.textContent = "Sending reply...";

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

            // Send seller reply
            const response = await fetch(
                `http://localhost:8080/api/enquiries/${enquiry.id}/reply`,
                {
                    method: "PUT",
                    credentials: "include",
                    headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-Token": csrfToken
                    },
                    body: JSON.stringify({ reply })
                }
            );

            const result = await response.json();

            if (!response.ok) {
                throw new Error(
                    result.message || "Unable to send reply."
                );
            }

            replyStatus.textContent = "Reply sent successfully!";

            // Reload enquiries to show updated status
            await loadSellerEnquiries();

        } catch (error) {
            console.error("Reply error:", error);
            replyStatus.textContent = error.message;
            replyButton.disabled = false;
        }
    });

    card.appendChild(replyForm);

} else if (enquiry.sellerReply) {

    // Display previously submitted reply
    const replyDisplay = document.createElement("div");
    replyDisplay.className = "seller-reply-display";

    const replyHeading = document.createElement("strong");
    replyHeading.textContent = "Your Reply:";

    const replyText = document.createElement("p");
    replyText.textContent = enquiry.sellerReply;

    replyDisplay.append(replyHeading, replyText);
    card.appendChild(replyDisplay);
}

list.appendChild(card);

        });

    } catch (error) {
        console.error("Seller enquiries error:", error);
        status.textContent = error.message;
    }
}

// Refresh enquiries manually
document.getElementById("refreshEnquiriesBtn")
    ?.addEventListener("click", loadSellerEnquiries);

// ======================================
// 9. INITIALIZE DASHBOARD
// ======================================


/* =====================================
   LOAD SELLER PROPERTY VISITS
===================================== */

/* UPDATE PROPERTY VISIT STATUS */

async function updateVisitStatus(visitId, newStatus) {
    if (!["COMPLETED", "CANCELLED"].includes(newStatus)) {
        return;
    }

    const action = newStatus === "COMPLETED"
        ? "mark this visit as completed"
        : "cancel this visit";

    if (!confirm(`Are you sure you want to ${action}?`)) {
        return;
    }

    try {
        // Get CSRF token
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            { credentials: "include" }
        );

        if (!csrfResponse.ok) {
            throw new Error("Unable to retrieve CSRF token.");
        }

        const { csrfToken } = await csrfResponse.json();

        // Update visit status
        const response = await fetch(
            `http://localhost:8080/api/visits/${visitId}/status`,
            {
                method: "PUT",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Failed to update visit."
            );
        }

        alert("Visit status updated successfully!");

        // Refresh the visits list
        await loadSellerVisits();

    } catch (error) {
        console.error("Visit status update error:", error);
        alert(error.message);
    }
}

async function loadSellerVisits() {
    const visitsList = document.getElementById("sellerVisitsList");

    if (!visitsList) return;

    visitsList.textContent = "Loading visit requests...";

    try {
        const response = await fetch(
            "http://localhost:8080/api/visits/seller",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load visit requests.");
        }

        const visits = await response.json();

        visitsList.replaceChildren();

        if (visits.length === 0) {
            visitsList.textContent = "No property visit requests yet.";
            return;
        }

        visits.forEach((visit) => {
            const card = document.createElement("div");
            card.className = "seller-visit-card";

            const heading = document.createElement("h3");
            heading.textContent = visit.propertyTitle;

            const details = [
                ["Buyer", visit.buyerName],
                ["Email", visit.buyerEmail],
                ["Visit Date", visit.visitDate],
                ["Visit Time", visit.visitTime],
                ["Status", visit.status],
                ["Remarks", visit.remarks || "None"]
            ];

            card.appendChild(heading);

            details.forEach(([label, value]) => {
                const paragraph = document.createElement("p");

                const strong = document.createElement("strong");
                strong.textContent = label + ": ";

                paragraph.appendChild(strong);
                paragraph.appendChild(
                    document.createTextNode(value ?? "")
                );

                card.appendChild(paragraph);
            });

// Show action buttons only for scheduled visits
if (visit.status === "SCHEDULED") {

    const actions = document.createElement("div");
    actions.className = "visit-actions";

    const completeBtn = document.createElement("button");
    completeBtn.type = "button";
    completeBtn.className = "visit-complete-btn";
    completeBtn.textContent = "Mark Completed";

    completeBtn.addEventListener("click", () => {
        updateVisitStatus(visit.id, "COMPLETED");
    });

    const cancelBtn = document.createElement("button");
    cancelBtn.type = "button";
    cancelBtn.className = "visit-cancel-btn";
    cancelBtn.textContent = "Cancel Visit";

    cancelBtn.addEventListener("click", () => {
        updateVisitStatus(visit.id, "CANCELLED");
    });

    actions.appendChild(completeBtn);
    actions.appendChild(cancelBtn);
    card.appendChild(actions);
}


            visitsList.appendChild(card);
        });

    } catch (error) {
        console.error("Seller visits error:", error);
        visitsList.textContent =
            "Unable to load property visit requests.";
    }
}

/* =====================================
   LOAD SELLER BOOKING REQUESTS
===================================== */

/* UPDATE BOOKING STATUS */

async function updateBookingStatus(bookingId, newStatus) {

    if (!["CONFIRMED", "CANCELLED"].includes(newStatus)) {
        return;
    }

    const action = newStatus === "CONFIRMED"
        ? "confirm"
        : "cancel";

    if (!confirm(`Are you sure you want to ${action} this booking?`)) {
        return;
    }

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

        // Update booking status
        const response = await fetch(
            `http://localhost:8080/api/bookings/${bookingId}/status`,
            {
                method: "PUT",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Failed to update booking."
            );
        }

        alert(result.message);

        // Refresh booking requests
        await loadSellerBookings();

        // Refresh property listing after confirmation
        await loadMyProperties();

    } catch (error) {
        console.error("Booking update error:", error);
        alert(error.message);
    }
}

/* =====================================
   FINALIZE PROPERTY SALE
===================================== */

async function finalizeSale(bookingId, button) {

    const confirmed = confirm(
        "Finalize this DEMO property sale?\n\n" +
        "The booking will become COMPLETED and " +
        "the property status will change to SOLD."
    );

    if (!confirmed) return;

    button.disabled = true;
    button.textContent = "Finalizing Sale...";

    try {

        // Fetch CSRF token
        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            {
                credentials: "include"
            }
        );

        if (!csrfResponse.ok) {
            throw new Error("Unable to retrieve CSRF token");
        }

        const { csrfToken } = await csrfResponse.json();

        // Finalize sale transaction
        const response = await fetch(
            `http://localhost:8080/api/transactions/sale/${bookingId}`,
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "X-CSRF-Token": csrfToken
                }
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Sale finalization failed"
            );
        }

        alert(
            "Demo Sale Completed Successfully!\n\n" +
            "Registration Reference: " +
            result.registrationNumber
        );

        // Refresh seller dashboard
        await loadSellerBookings();
        await loadMyProperties();

    } catch (error) {

        console.error("Finalize sale error:", error);
        alert(error.message);

    } finally {

        button.disabled = false;
        button.textContent = "Finalize Sale";
    }
}

/* =====================================
   CREATE RENTAL AGREEMENT
===================================== */

async function createRentalAgreement(bookingId, button) {

    const startDate = prompt(
        "Enter rental start date (YYYY-MM-DD):"
    );

    if (startDate === null) return;

    const endDate = prompt(
        "Enter rental end date (YYYY-MM-DD):"
    );

    if (endDate === null) return;

    const monthlyRent = prompt(
        "Enter monthly rent (₹):"
    );

    if (monthlyRent === null) return;

    const securityDeposit = prompt(
        "Enter security deposit (₹):"
    );

    if (securityDeposit === null) return;

    // Validate input
    const datePattern = /^\d{4}-\d{2}-\d{2}$/;

    const validDate = (value) => {
        if (!datePattern.test(value)) return false;

        const date = new Date(value + "T00:00:00");

        return !Number.isNaN(date.getTime()) &&
            date.getFullYear() === Number(value.slice(0, 4)) &&
            date.getMonth() + 1 === Number(value.slice(5, 7)) &&
            date.getDate() === Number(value.slice(8, 10));
    };

    const validAmount = (value, allowZero = false) => {
        if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) {
            return false;
        }

        const amount = Number(value);

        return Number.isFinite(amount) &&
            amount <= 9999999999.99 &&
            (allowZero ? amount >= 0 : amount > 0);
    };

    if (
        !validDate(startDate) ||
        !validDate(endDate) ||
        endDate <= startDate ||
        !validAmount(monthlyRent) ||
        !validAmount(securityDeposit, true)
    ) {
        alert("Please enter valid dates and rental amounts.");
        return;
    }

    if (!confirm(
        "Create this DEMO rental agreement?\n\n" +
        "The booking will become COMPLETED and " +
        "the property will become RENTED."
    )) {
        return;
    }

    button.disabled = true;
    button.textContent = "Creating Agreement...";

    try {

        const csrfResponse = await fetch(
            "http://localhost:8080/api/auth/csrf",
            {
                credentials: "include"
            }
        );

        if (!csrfResponse.ok) {
            throw new Error("Unable to retrieve CSRF token.");
        }

        const { csrfToken } = await csrfResponse.json();

        const response = await fetch(
            `http://localhost:8080/api/transactions/rental/${bookingId}`,
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-Token": csrfToken
                },
                body: JSON.stringify({
                    startDate,
                    endDate,
                    monthlyRent: Number(monthlyRent),
                    securityDeposit: Number(securityDeposit)
                })
            }
        );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || "Rental agreement creation failed."
            );
        }

        alert(
            "Demo Rental Agreement Created Successfully!\n\n" +
            "Booking Status: " + result.bookingStatus + "\n" +
            "Property Status: " + result.propertyStatus + "\n" +
            "Agreement Status: " + result.agreementStatus
        );

        await loadSellerBookings();
        await loadMyProperties();

    } catch (error) {

        console.error("Rental agreement error:", error);
        alert(error.message);

    } finally {

        button.disabled = false;
        button.textContent = "Create Rental Agreement";
    }
}

/* =====================================
   SELLER TRANSACTION HISTORY
===================================== */

async function loadSellerTransactionHistory() {

    const salesList =
        document.getElementById("sellerSalesList");

    const rentalsList =
        document.getElementById("sellerRentalsList");

    if (!salesList || !rentalsList) return;

    salesList.textContent = "Loading completed sales...";
    rentalsList.textContent = "Loading rental agreements...";

    const formatCurrency = (amount) => {
        return Number(amount).toLocaleString("en-IN", {
            style: "currency",
            currency: "INR"
        });
    };

    // Safely create a text row
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
            "http://localhost:8080/api/transactions/seller",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error(
                "Unable to load seller transaction history."
            );
        }

        const data = await response.json();

        salesList.replaceChildren();
        rentalsList.replaceChildren();

        // ============================
        // COMPLETED PROPERTY SALES
        // ============================

        if (!data.sales || data.sales.length === 0) {

            salesList.textContent =
                "No completed property sales found.";

        } else {

            data.sales.forEach((sale) => {

                const card = document.createElement("div");
                card.className = "seller-transaction-card";

                const heading = document.createElement("h4");
                heading.textContent = sale.propertyTitle;

                card.appendChild(heading);

                addDetail(card, "Sale ID", sale.transactionId);
                addDetail(card, "Booking ID", sale.bookingId);
                addDetail(card, "Buyer Name", sale.buyerName);
                addDetail(card, "Buyer Email", sale.buyerEmail);
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
                card.className = "seller-transaction-card";

                const heading = document.createElement("h4");
                heading.textContent = rental.propertyTitle;

                card.appendChild(heading);

                addDetail(
                    card,
                    "Agreement ID",
                    rental.transactionId
                );

                addDetail(card, "Booking ID", rental.bookingId);
                addDetail(card, "Tenant Name", rental.tenantName);
                addDetail(card, "Tenant Email", rental.tenantEmail);
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

        console.error(
            "Seller transaction history error:",
            error
        );

        salesList.textContent =
            "Unable to load completed sales.";

        rentalsList.textContent =
            "Unable to load rental agreements.";
    }
}

async function loadSellerBookings() {

    const bookingsList =
        document.getElementById("sellerBookingsList");

    if (!bookingsList) return;

    bookingsList.textContent = "Loading booking requests...";

    try {
        const response = await fetch(
            "http://localhost:8080/api/bookings/seller",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load booking requests.");
        }

        const bookings = await response.json();

        bookingsList.replaceChildren();

        if (bookings.length === 0) {
            bookingsList.textContent =
                "No property booking requests yet.";
            return;
        }

        bookings.forEach((booking) => {

            const card = document.createElement("div");
            card.className = "seller-booking-card";

            const heading = document.createElement("h3");
            heading.textContent = booking.propertyTitle;
            card.appendChild(heading);

            const details = [
                ["Buyer", booking.buyerName],
                ["Email", booking.buyerEmail],
                ["Booking Type", booking.bookingType],
                ["Booking Amount",
                    Number(booking.bookingAmount)
                        .toLocaleString("en-IN", {
                            style: "currency",
                            currency: "INR"
                        })],
                ["Booking Date", booking.bookingDate],
                ["Status", booking.status]
            ];

            details.forEach(([label, value]) => {

                const paragraph = document.createElement("p");

                const strong = document.createElement("strong");
                strong.textContent = label + ": ";

                paragraph.appendChild(strong);
                paragraph.appendChild(
                    document.createTextNode(value ?? "")
                );

                card.appendChild(paragraph);
            });

// Show buttons only for pending bookings
if (booking.status === "PENDING") {

    const actions = document.createElement("div");
    actions.className = "booking-actions";

    const confirmBtn = document.createElement("button");
    confirmBtn.type = "button";
    confirmBtn.className = "booking-confirm-btn";
    confirmBtn.textContent = "Confirm Booking";

    const cancelBtn = document.createElement("button");
    cancelBtn.type = "button";
    cancelBtn.className = "booking-cancel-btn";
    cancelBtn.textContent = "Cancel Booking";

    confirmBtn.addEventListener("click", async () => {
        confirmBtn.disabled = true;
        cancelBtn.disabled = true;

        await updateBookingStatus(booking.id, "CONFIRMED");

        confirmBtn.disabled = false;
        cancelBtn.disabled = false;
    });

    cancelBtn.addEventListener("click", async () => {
        confirmBtn.disabled = true;
        cancelBtn.disabled = true;

        await updateBookingStatus(booking.id, "CANCELLED");

        confirmBtn.disabled = false;
        cancelBtn.disabled = false;
    });

    actions.appendChild(confirmBtn);
    actions.appendChild(cancelBtn);
    card.appendChild(actions);
}


// Show Finalize Sale only for confirmed SALE bookings
if (
    booking.status === "CONFIRMED" &&
    booking.bookingType === "SALE"
) {

    const finalizeBtn = document.createElement("button");

    finalizeBtn.type = "button";
    finalizeBtn.className = "finalize-sale-btn";
    finalizeBtn.textContent = "Finalize Sale";

    finalizeBtn.addEventListener("click", () => {
        finalizeSale(booking.id, finalizeBtn);
    });

    card.appendChild(finalizeBtn);
}

// Show rental agreement button for confirmed RENT bookings
if (
    booking.status === "CONFIRMED" &&
    booking.bookingType === "RENT"
) {

    const rentalBtn = document.createElement("button");

    rentalBtn.type = "button";
    rentalBtn.className = "create-rental-btn";
    rentalBtn.textContent = "Create Rental Agreement";

    rentalBtn.addEventListener("click", () => {
        createRentalAgreement(booking.id, rentalBtn);
    });

    card.appendChild(rentalBtn);
}

            bookingsList.appendChild(card);
        });

    } catch (error) {
        console.error("Seller bookings error:", error);

        bookingsList.textContent =
            "Unable to load property booking requests.";
    }
}

initializeSellerDashboard();
