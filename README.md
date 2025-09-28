# Dorz Book a Ride MVP

This is the MVP (Minimum Viable Product) for a Ride Booking feature to be added into the Dorz App.

This implementation focuses on integrating the **Yango** ride-hailing service as a proof-of-concept.

## 1. Core Functionality

The application allows a user to:
*   Select a starting point and a destination for a ride.
*   Fetch available ride options (e.g., Economy, Business) and their estimated prices from the Yango API.
*   Proceed through a simulated payment and booking flow.
*   View a history of their past and ongoing rides.
*   Save and manage frequently used addresses (e.g., Home, Work).

## 2. Features

*   **Dynamic Location Selection**: Users can select ride locations in three ways:
    *   **Interactive Map**: Tapping on the map to choose precise coordinates.
    *   **Place Search**: Using Google Places API for location search and autocomplete.
    *   **Saved Addresses**: Quickly selecting from a list of pre-saved locations.
*   **Clipboard Pasting**: Users can paste a Google Maps link directly into the app to set a start or destination point. The app parses the URL to extract the coordinates.
*   **Real-time Ride Options**: Fetches live data from the Yango API to display available ride classes, estimated prices, and wait times.
*   **Simulated Booking Flow**: Includes a payment screen (with mock card validation) and a ride confirmation screen to demonstrate a complete user journey.
*   **Simulated Ride Tracking**: A map-based screen that shows the route and simulates the driver's vehicle moving from the start point to the destination.
*   **Persistent Data**:
    *   **Ride History**: All booked rides are saved to a local Room database, separated into "Ongoing" and "Previous" rides.
    *   **Saved Addresses**: User-named addresses are stored locally for quick access.

## 3. Limitations & Technical Notes

*   **API Provider**: This MVP exclusively integrates the **Yango API**. Integration with other providers like Uber or Careem was not possible, as they did not respond to access requests. Bolt's API is also not publicly available.
*   **Simulated Booking**: The "Confirm Ride" functionality is a simulation. As per Yandex/Yango API documentation, third-party apps are not permitted to book a ride directly. The intended behavior is for the button to redirect the user to the official Yango app with the route pre-filled. However, to demonstrate a full-featured flow for this MVP, I implemented a complete, self-contained (but non-functional) booking, payment, and tracking experience within the Dorz app.
*   **Static Driver/Car Info**: The driver information (name, car model) and ETA on the tracking screen are hardcoded for demonstration purposes.

## 4. How to Use (User Flow)

1.  **Launch the App**: The user is presented with the home screen.
2.  **Start a Booking**: From the home screen, tap "Instant Ride".
3.  **Set Locations**:
    *   Tap on the "Select starting point" or "Select destination" fields.
    *   On the map screen, either search for a location or tap on the map.
    *   Alternatively, use the "Use Saved Address" button to select a pre-saved place.
    *   Another option is to copy a Google Maps link and use the "Paste" icon on the booking screen.
4.  **View Ride Options**: Once both start and destination are set, the app automatically fetches and displays a list of available Yango rides with their prices.
5.  **Select a Ride**: Tap on a desired ride option (e.g., "Economy").
6.  **Confirm and Pay**:
    *   The app navigates to the payment screen, showing a summary of the ride.
    *   The user can select a mock payment method (e.g., Cash or Card). The card form includes basic input validation.
    *   Tap "Confirm Ride".
7.  **Ride Confirmed**: The user sees a confirmation screen. From here, they can either:
    *   Tap **"Track Your Ride"** to view the simulated map tracking screen.
    *   Tap **"Back to Home"** to return to the main menu.
8.  **View History**: From the home screen, navigate to "Your Rides" to see a list of ongoing and completed rides.

