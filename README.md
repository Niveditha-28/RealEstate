
# RealEstate Hub

A full-stack Real Estate Management System developed using
Spring Boot, MySQL, HTML, CSS, and JavaScript.

The application provides separate Buyer and Seller dashboards
for property listings, enquiries, visits, bookings, payments,
and property transactions.

## Features

### Buyer
- User registration and login
- Search and filter properties
- View property details and image galleries
- Contact sellers through enquiries
- Schedule property visits
- Submit property booking requests
- Complete simulated payments
- View purchase and rental transaction history

### Seller
- User registration and login
- Add, edit, and manage property listings
- Upload multiple property images
- View and respond to buyer enquiries
- Manage property visit requests
- Confirm or cancel property bookings
- Finalize property sales
- Create rental agreements
- View transaction history

## Technology Stack

| Layer | Technology |
|-------|------------|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java, Spring Boot |
| Database | MySQL |
| Database Connectivity | JDBC |
| Build Tool | Maven |
| Development Environment | VS Code |

## Database Design

The system uses 10 relational tables:

1. users
2. property_type
3. property
4. property_image
5. enquiry
6. property_visit
7. booking
8. payment
9. sale_transaction
10. rental_agreement

The database uses primary keys, foreign keys,
and relationships to maintain data integrity.

## Project Structure

RealEstate-Hub/
- frontend/ - Buyer, Seller and Authentication UI
- realestate-api/ - Spring Boot REST API
- src/ - Java DAO and database connectivity classes
- .gitignore - Git exclusions
- README.md - Project documentation

## Application Workflow

1. Seller registers and lists a property.
2. Buyer searches and views available properties.
3. Buyer sends an enquiry or schedules a visit.
4. Seller responds to the enquiry and manages the visit.
5. Buyer submits a property booking request.
6. Seller confirms the booking.
7. Buyer completes a simulated payment.
8. Seller finalizes the sale or creates a rental agreement.
9. Both users can view their transaction history.

## Running the Project

### Backend

1. Install Java, Maven, and MySQL.
2. Create and configure the required MySQL database.
3. Configure database connectivity using local environment
   variables or application configuration.
4. Open the `realestate-api` folder.
5. Run:

   ./mvnw spring-boot:run

### Frontend

1. Open the `frontend` folder in VS Code.
2. Start the frontend using Live Server.
3. Open:

   http://localhost:5500/frontend/login.html

Ensure the Spring Boot backend is running on port 8080.

## Screenshots

Screenshots of the application will be added here.

## Security Note

Payments are simulated for academic demonstration.
No real payment gateway is integrated.

## Project Type

Database Management Systems (DBMS) Academic Project
