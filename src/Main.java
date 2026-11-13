import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n======================================");
            System.out.println("   REAL ESTATE MANAGEMENT SYSTEM");
            System.out.println("======================================");
            System.out.println("1. View All Properties");
            System.out.println("2. Search Properties by City");
            System.out.println("3. Add Property");
            System.out.println("4. Create Booking");
            System.out.println("5. View Bookings");
            System.out.println("6. Make Payment");
            System.out.println("7. View Payments");
            System.out.println("8. Complete Sale");
            System.out.println("9. Create Rental Agreement");
            System.out.println("10.  Create Enquiry");
            System.out.println("11.  View Enquiries");
            System.out.println("12. Schedule Property Visit");
            System.out.println("13. View Property Visits");
            System.out.println("14. Update Visit Status");
            System.out.println("15. Exit");
            System.out.println("======================================");
            System.out.print("Enter your choice: ");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    PropertyDAO.viewAllProperties();
                    break;

                case 2:
                    System.out.print("Enter city: ");
                    String city = sc.nextLine();

                    PropertyDAO.searchByCity(city);
                    break;

                case 3:

                    System.out.print("Owner ID: ");
                    int ownerId = sc.nextInt();

                    System.out.print("Agent ID: ");
                    int agentId = sc.nextInt();

                    System.out.print("Property Type ID: ");
                    int typeId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Title: ");
                    String title = sc.nextLine();

                    System.out.print("Description: ");
                    String description = sc.nextLine();

                    System.out.print("Address: ");
                    String address = sc.nextLine();

                    System.out.print("City: ");
                    String cityName = sc.nextLine();

                    System.out.print("Price: ");
                    double price = sc.nextDouble();

                    System.out.print("Area: ");
                    double area = sc.nextDouble();

                    System.out.print("Bedrooms: ");
                    int bedrooms = sc.nextInt();

                    System.out.print("Bathrooms: ");
                    int bathrooms = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Listing Type (SALE/RENT): ");
                    String listingType = sc.nextLine().toUpperCase();

                    PropertyDAO.addProperty(
                            ownerId,
                            agentId,
                            typeId,
                            title,
                            description,
                            address,
                            cityName,
                            price,
                            area,
                            bedrooms,
                            bathrooms,
                            listingType
                    );

                    break;

                case 4:

                    System.out.print("Buyer ID: ");
                    int buyerId = sc.nextInt();

                    System.out.print("Property ID: ");
                    int propertyId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Booking Type (SALE/RENT): ");
                    String bookingType = sc.nextLine();

                    System.out.print("Booking Amount: ");
                    double bookingAmount = sc.nextDouble();
                    sc.nextLine();

                    BookingDAO.createBooking(
                            buyerId,
                            propertyId,
                            bookingType,
                            bookingAmount
                    );

                    break;

                case 5:
                    BookingDAO.viewBookings();
                    break;

                case 6:

                    System.out.print("Booking ID: ");
                    int paymentBookingId = sc.nextInt();

                    System.out.print("Amount: ");
                    double paymentAmount = sc.nextDouble();
                    sc.nextLine();

                    System.out.print(
                        "Payment Method (CASH/CARD/UPI/BANK_TRANSFER): "
                    );
                    String paymentMethod = sc.nextLine();

                    System.out.print("Transaction ID: ");
                    String transactionId = sc.nextLine();

                    PaymentDAO.makePayment(
                        paymentBookingId,
                        paymentAmount,
                        paymentMethod,
                        transactionId
                    );

                    break;

                case 7:
                    PaymentDAO.viewPayments();
                    break;

                case 8:
                    System.out.print("Booking ID: ");
                    int saleBookingId = sc.nextInt();

                    System.out.print("Final Sale Price: ");
                    double finalPrice = sc.nextDouble();
                    sc.nextLine();

                    System.out.print("Registration Number: ");
                    String registrationNumber = sc.nextLine();

                    SaleDAO.completeSale(
                        saleBookingId,
                        finalPrice,
                        registrationNumber
                    );

                    break;

                case 9:

                    System.out.print("Booking ID: ");
                    int rentalBookingId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Start Date (YYYY-MM-DD): ");
                    String startDate = sc.nextLine();

                    System.out.print("End Date (YYYY-MM-DD): ");
                    String endDate = sc.nextLine();

                    System.out.print("Monthly Rent: ");
                    double monthlyRent = sc.nextDouble();

                    System.out.print("Security Deposit: ");
                    double securityDeposit = sc.nextDouble();
                    sc.nextLine();

                    RentalDAO.createRentalAgreement(
                        rentalBookingId,
                        startDate,
                        endDate,
                        monthlyRent,
                        securityDeposit
                    );

                    break;

                case 10:
                    

                    System.out.print("Buyer ID: ");
                    int enquiryBuyerId = sc.nextInt();

                    System.out.print("Property ID: ");
                    int enquiryPropertyId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter enquiry message: ");
                    String enquiryMessage = sc.nextLine();

                    EnquiryDAO.createEnquiry(
                        enquiryBuyerId,
                        enquiryPropertyId,
                        enquiryMessage
                    );

                    break;

                case 11:
                    EnquiryDAO.viewEnquiries();
                    break;

                case 12:

                    System.out.print("Buyer ID: ");
                    int visitBuyerId = sc.nextInt();

                    System.out.print("Property ID: ");
                    int visitPropertyId = sc.nextInt();

                    System.out.print("Agent ID: ");
                    int visitAgentId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Visit Date (YYYY-MM-DD): ");
                    String visitDate = sc.nextLine();

                    System.out.print("Visit Time (HH:MM:SS): ");
                    String visitTime = sc.nextLine();

                    VisitDAO.scheduleVisit(
                        visitBuyerId,
                        visitPropertyId,
                        visitAgentId,
                        visitDate,
                        visitTime
                    );

                    break;

                case 13:
                    VisitDAO.viewVisits();
                    break;

                case 14:

                    System.out.print("Visit ID: ");
                    int visitId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Status (SCHEDULED/COMPLETED/CANCELLED): ");
                    String visitStatus = sc.nextLine();

                    System.out.print("Remarks: ");
                    String remarks = sc.nextLine();

                    VisitDAO.updateVisitStatus(
                        visitId,
                        visitStatus,
                        remarks
                    );

                    break;

                case 15:
                    System.out.println("Exiting application...");
                    break;
            }

        } while (choice != 15);

        sc.close();
    }
}