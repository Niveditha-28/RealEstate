
"use strict";

const API_URL = "http://localhost:8080/api/properties";

const propertyContainer =
    document.getElementById("propertyContainer");

const cityInput =
    document.getElementById("cityInput");

const listingType =
    document.getElementById("listingType");

const searchBtn =
    document.getElementById("searchBtn");

let properties = [];

// Format property prices
function formatPrice(price, type) {
    const formatted = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    }).format(price);

    return type === "RENT"
        ? formatted + " / month"
        : formatted;
}

// Display property cards
function displayProperties(propertyList) {
    propertyContainer.replaceChildren();

    if (propertyList.length === 0) {
        const message = document.createElement("p");
        message.textContent = "No properties found.";
        propertyContainer.appendChild(message);
        return;
    }

    propertyList.forEach(property => {
        const card = document.createElement("div");
        card.className = "property-card";

        const image = document.createElement("img");
        image.alt = property.title;
image.loading = "lazy";

image.onerror = function () {
    console.error("Image failed:", image.src);
    image.onerror = null;
    image.src = "https://placehold.co/600x400?text=No+Image";
};

image.src = property.image ||
    "https://placehold.co/600x400?text=No+Image";

        const info = document.createElement("div");
        info.className = "property-info";

        const title = document.createElement("h3");
        title.textContent = property.title;

        const city = document.createElement("p");
        city.textContent = "Location: " + property.city;

        const type = document.createElement("p");
        type.textContent = "Type: " +
            (property.type === "SALE" ? "For Sale" : "For Rent");

        const bedrooms = document.createElement("p");
        bedrooms.textContent =
            "Bedrooms: " + (property.bedrooms ?? "N/A");

        const status = document.createElement("p");
        status.textContent = "Status: " + property.status;

        const price = document.createElement("p");
        price.className = "property-price";
        price.textContent = formatPrice(
            property.price,
            property.type
        );

       
const detailsButton = document.createElement("button");
detailsButton.className = "details-btn";
detailsButton.textContent = "View Details";

detailsButton.addEventListener("click", () => {
    showPropertyDetails(property);
});

info.append(
    title,
    city,
    type,
    bedrooms,
    status,
    price,
    detailsButton
);


        card.append(image, info);
        propertyContainer.appendChild(card);
    });
}

// Fetch properties from Spring Boot
async function loadProperties() {
    propertyContainer.textContent = "Loading properties...";

    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error("HTTP error: " + response.status);
        }

        properties = await response.json();

        displayProperties(properties);

    } catch (error) {
        console.error("Error loading properties:", error);

        propertyContainer.textContent =
            "Unable to load properties. Check if Spring Boot is running.";
    }
}

// Search properties
function searchProperties() {
    const city = cityInput.value.trim().toLowerCase();
    const type = listingType.value;

    const filtered = properties.filter(property => {
        const matchesCity =
            property.city.toLowerCase().includes(city);

        const matchesType =
            type === "" || property.type === type;

        return matchesCity && matchesType;
    });

    displayProperties(filtered);
}

searchBtn.addEventListener("click", searchProperties);

cityInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        searchProperties();
    }
});

// Load database properties when the page opens
loadProperties();

const propertyModal =
    document.getElementById("propertyModal");

const modalDetails =
    document.getElementById("modalDetails");

const closeModal =
    document.getElementById("closeModal");

function showPropertyDetails(property) {
    modalDetails.replaceChildren();

    const heading = document.createElement("h2");
    heading.textContent = property.title;

    const image = document.createElement("img");
    image.src = property.image ||
        "http://localhost:8080/images/apartment.jpg";
    image.alt = property.title;
    image.className = "modal-image";

    const fields = [
        ["Description", property.description],
        ["Address", property.address],
        ["City", property.city],
        ["Area", property.area],
        ["Bedrooms", property.bedrooms],
        ["Bathrooms", property.bathrooms],
        ["Listing Type", property.type],
        ["Status", property.status],
        ["Price", formatPrice(property.price, property.type)]
    ];

    modalDetails.append(heading, image);

    fields.forEach(([label, value]) => {
        const paragraph = document.createElement("p");
        paragraph.textContent =
            label + ": " + (value ?? "N/A");
        modalDetails.appendChild(paragraph);
    });

    propertyModal.classList.add("show");
}

closeModal.addEventListener("click", () => {
    propertyModal.classList.remove("show");
});

propertyModal.addEventListener("click", event => {
    if (event.target === propertyModal) {
        propertyModal.classList.remove("show");
    }
});
