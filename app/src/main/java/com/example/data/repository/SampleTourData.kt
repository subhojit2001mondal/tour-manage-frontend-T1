package com.example.data.repository

import com.example.data.models.*
import com.google.firebase.Timestamp
import java.util.Calendar

object SampleTourData {
    fun getSampleDestinations(): List<Destination> = listOf(
        Destination(
            id = "dest_kashmir",
            name = "Srinagar & Gulmarg Paradise",
            state = "Jammu & Kashmir",
            region = "North",
            description = "Experience Heaven on Earth with tranquil Dal Lake Shikara rides, snow-capped Pir Panjal ranges, and pine forests in Gulmarg and Pahalgam.",
            coverImageUrl = "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?auto=format&fit=crop&w=1200&q=80",
                "https://images.unsplash.com/photo-1566837945700-30057527ade0?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80"
            ),
            bestSeason = "April to October & Winter Snow (Dec-Feb)",
            tags = listOf("Snow", "Romantic", "Lakes", "Mountains", "Houseboat"),
            featured = true,
            active = true,
            isDemo = true
        ),
        Destination(
            id = "dest_kerala",
            name = "Alleppey & Munnar Serenity",
            state = "Kerala",
            region = "South",
            description = "Emerald green tea rolling hills in Munnar followed by an overnight cruise across palm-fringed backwaters on a traditional Kettuvallam houseboat.",
            coverImageUrl = "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80",
                "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=800&q=80"
            ),
            bestSeason = "September to March",
            tags = listOf("Backwaters", "Tea Gardens", "Ayurveda", "Houseboat"),
            featured = true,
            active = true,
            isDemo = true
        ),
        Destination(
            id = "dest_rajasthan",
            name = "Royal Jaipur & Udaipur Forts",
            state = "Rajasthan",
            region = "West",
            description = "Immerse in the grand heritage of Rajput monarchs, stunning sandstone palaces, desert cultural nights, and sunset boat rides over Lake Pichola.",
            coverImageUrl = "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1200&q=80",
                "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=800&q=80"
            ),
            bestSeason = "October to March",
            tags = listOf("Heritage", "Palaces", "Culture", "Forts", "Desert"),
            featured = true,
            active = true,
            isDemo = true
        ),
        Destination(
            id = "dest_goa",
            name = "Goa Coastal Sun & Heritage",
            state = "Goa",
            region = "West",
            description = "Golden sandy coastlines, Portuguese colonial churches in Old Goa, spice plantations, and vibrant beach shacks.",
            coverImageUrl = "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80"
            ),
            bestSeason = "November to February",
            tags = listOf("Beach", "Nightlife", "Water Sports", "Seafood"),
            featured = false,
            active = true,
            isDemo = true
        ),
        Destination(
            id = "dest_meghalaya",
            name = "Meghalaya & Living Root Bridges",
            state = "Meghalaya",
            region = "Northeast",
            description = "The abode of clouds: crystal clear Umngot river in Dawki, sacred groves of Mawphlang, Nohkalikai waterfalls, and ancient double decker root bridges.",
            coverImageUrl = "https://images.unsplash.com/photo-1626014303757-64660a927a71?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1626014303757-64660a927a71?auto=format&fit=crop&w=1200&q=80"
            ),
            bestSeason = "October to April",
            tags = listOf("Waterfalls", "Trekking", "Nature", "Caves"),
            featured = true,
            active = true,
            isDemo = true
        ),
        Destination(
            id = "dest_andaman",
            name = "Andaman Coral Islands & Radhanagar",
            state = "Andaman & Nicobar",
            region = "Islands",
            description = "Pristine white sand beaches, vibrant coral reef scuba diving, and historical light & sound shows at Cellular Jail.",
            coverImageUrl = "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80"
            ),
            bestSeason = "October to May",
            tags = listOf("Scuba", "Islands", "Beaches", "Coral Reefs"),
            featured = false,
            active = true,
            isDemo = true
        )
    )

    fun getSampleAgencies(): List<Agency> = listOf(
        Agency(
            id = "ag_himalayan",
            name = "Himalayan Foothills Expeditions",
            logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=200&q=80",
            description = "Specializing in high-altitude tours, Kashmir valley luxury circuits, and certified mountain guide services for over 14 years.",
            tier = "premium",
            rating = 4.9,
            verified = true,
            phone = "+91 94191 12345",
            email = "ops@himalayanexpeditions.in",
            city = "Srinagar",
            commissionPercent = 10.0,
            active = true,
            isDemo = true
        ),
        Agency(
            id = "ag_royalmarwar",
            name = "Royal Marwar Heritage Tours",
            logoUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80",
            description = "Direct custodian connections with heritage palaces, desert camp hosts, and chauffeur-driven luxury sedans across Rajasthan.",
            tier = "premium",
            rating = 4.8,
            verified = true,
            phone = "+91 98290 54321",
            email = "support@royalmarwartours.com",
            city = "Jaipur",
            commissionPercent = 10.0,
            active = true,
            isDemo = true
        ),
        Agency(
            id = "ag_malabar",
            name = "Malabar Coast & Backwater Escapes",
            logoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80",
            description = "Family-owned houseboat fleet and eco-resort operators offering authentic spice trail & backwater hospitality.",
            tier = "standard",
            rating = 4.7,
            verified = true,
            phone = "+91 94471 88776",
            email = "info@malabarescapes.in",
            city = "Kochi",
            commissionPercent = 10.0,
            active = true,
            isDemo = true
        ),
        Agency(
            id = "ag_budgetbharat",
            name = "Bharat Yatra Travels",
            logoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80",
            description = "Economical, group-friendly holiday packages with tempo travellers and central budget hotels across all popular tourist hubs.",
            tier = "budget",
            rating = 4.5,
            verified = true,
            phone = "+91 98110 33445",
            email = "bookings@bharatyatra.com",
            city = "New Delhi",
            commissionPercent = 10.0,
            active = true,
            isDemo = true
        )
    )

    fun getSamplePackages(): List<TourPackage> = listOf(
        TourPackage(
            id = "pkg_kashmir_luxury",
            agencyId = "ag_himalayan",
            destinationId = "dest_kashmir",
            title = "Kashmir Royale: Dal Houseboat, Gulmarg & Pahalgam Valley",
            days = 6,
            nights = 5,
            pricePerPerson = 28500L,
            inclusions = listOf(
                "Exclusive Private AC Innova Crysta for entire tour",
                "1 Night Deluxe Heritage Dal Lake Houseboat stay",
                "4 Nights in 4-Star Mountain View Resorts",
                "Daily Buffet Breakfast and 4-Course Dinners",
                "1-Hour Complimentary Shikara Ride with Kahwa Tea",
                "Gondola Phase 1 Cable Car Ticket Assistance",
                "24x7 Dedicated Trip Coordinator in Srinagar"
            ),
            exclusions = listOf(
                "Airfare to/from Srinagar Airport",
                "Pony rides in Gulmarg / Baisaran",
                "Gondola Phase 2 ticket",
                "Personal shopping & laundry expenses"
            ),
            itinerary = listOf(
                ItineraryDay(1, "Arrival Srinagar & Dal Lake Shikara", "Airport pickup by executive chauffeur. Check-in to Royal Houseboat. Evening sunset Shikara ride across floating gardens and Char Chinar with Kashmiri Kahwa."),
                ItineraryDay(2, "Srinagar to Gulmarg Meadow of Flowers", "Scenic drive through Tangmarg pine hills. Board Gulmarg Gondola for panoramic snow-view of Mount Apharwat. Overnight at Khyber-view resort."),
                ItineraryDay(3, "Gulmarg to Pahalgam Valley of Shepherds", "Drive through saffron fields of Pampore and Awantipora ruins. Arrive at Lidder River bank hotel. Enjoy fresh trout dinner."),
                ItineraryDay(4, "Pahalgam Betaab Valley & Aru Exploration", "Visit Betaab Valley, Chandanwari (start of Amarnath Yatra), and Aru Valley. Evening leisure walk around Pahalgam local market."),
                ItineraryDay(5, "Return to Srinagar & Mughal Gardens Tour", "Visit Nishat Bagh (Garden of Bliss) and Shalimar Bagh (Abode of Love). Traditional wazwan or multicuisine dinner."),
                ItineraryDay(6, "Airport Departure with Warm Memories", "Morning breakfast. Chauffeur drop-off to Srinagar International Airport.")
            ),
            cancellationPolicy = "Free cancellation up to 15 days before departure. 50% refund between 7 to 14 days. Non-refundable within 7 days.",
            maxGroupSize = 6,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1566837945700-30057527ade0?auto=format&fit=crop&w=800&q=80"
            ),
            rating = 4.9,
            active = true,
            vehicle = VehicleInfo(
                type = "SUV",
                vehicleName = "Toyota Innova Crysta (Dual AC)",
                ac = true,
                seatingCapacity = 6,
                stationOrAirportPickup = true,
                details = "Commercial yellow plate tourist vehicle with luggage carrier, mobile chargers, first-aid kit, and an experienced Kashmiri chauffeur speaking English, Hindi & Urdu.",
                imageUrl = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80"
            ),
            food = FoodInfo(
                mealPlan = "Breakfast + Dinner",
                cuisine = "Veg & Non-veg",
                jainOnRequest = true,
                details = "Lavish buffet dinners featuring Kashmiri Rogan Josh, Paneer Dum Aloo, fresh saffron pulao, Phirni, alongside Continental & South Indian breakfast."
            ),
            hotel = HotelInfo(
                hotelName = "Mirani Heritage Houseboat & Grand Pine Resort",
                category = "4-star",
                roomType = "Premium Lake & Mountain View Room",
                occupancy = "Double sharing",
                amenities = listOf("Central Heating", "High-speed Wi-Fi", "Electric Blankets", "Geyser Hot Water", "Room Service", "Tea Maker"),
                details = "Handcrafted cedar wood carved houseboats on Dal Lake and boutique mountain-facing luxury properties in Gulmarg and Pahalgam.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1566837945700-30057527ade0?auto=format&fit=crop&w=800&q=80"
                )
            ),
            isDemo = true
        ),
        TourPackage(
            id = "pkg_kashmir_budget",
            agencyId = "ag_budgetbharat",
            destinationId = "dest_kashmir",
            title = "Kashmir Explorer: Budget Valley Wonders",
            days = 5,
            nights = 4,
            pricePerPerson = 16999L,
            inclusions = listOf(
                "Shared AC Tempo Traveller / Swift Dzire transfers",
                "1 Night Houseboat + 3 Nights Deluxe Budget Hotel",
                "Daily Breakfast Included",
                "All toll taxes, parking, and driver allowances",
                "Sightseeing of Srinagar, Gulmarg & Sonmarg"
            ),
            exclusions = listOf(
                "Lunches and Dinners",
                "Gondola and pony ride charges",
                "Entry tickets to Mughal gardens"
            ),
            itinerary = listOf(
                ItineraryDay(1, "Srinagar Arrival & Houseboat Stay", "Check-in to Nigeen Lake Houseboat. Relax and admire the sunset."),
                ItineraryDay(2, "Day excursion to Sonmarg (Meadow of Gold)", "Drive through Sindh Valley to Sonmarg. Thajiwas Glacier viewpoint."),
                ItineraryDay(3, "Gulmarg Day Trip", "Excursion to Gulmarg snow slopes. Return to Srinagar hotel."),
                ItineraryDay(4, "Pahalgam Day Trip", "Scenic drive along Lidder river. Evening shopping in Lal Chowk."),
                ItineraryDay(5, "Departure", "Drop-off at Srinagar Airport.")
            ),
            cancellationPolicy = "Free cancellation up to 7 days before departure.",
            maxGroupSize = 12,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1595815771614-ade9d652a65d?auto=format&fit=crop&w=1000&q=80"
            ),
            rating = 4.4,
            active = true,
            vehicle = VehicleInfo(
                type = "Sedan",
                vehicleName = "Maruti Suzuki Dzire AC",
                ac = true,
                seatingCapacity = 4,
                stationOrAirportPickup = true,
                details = "Clean AC Sedan for couples and small families.",
                imageUrl = ""
            ),
            food = FoodInfo(
                mealPlan = "Breakfast only",
                cuisine = "Veg & Non-veg",
                jainOnRequest = true,
                details = "Fresh morning breakfast with hot puri bhaji, eggs, parathas, toast, tea and coffee."
            ),
            hotel = HotelInfo(
                hotelName = "Hotel Valley Blossom & Nigeen Houseboat",
                category = "Budget",
                roomType = "Standard Deluxe Room",
                occupancy = "Double sharing",
                amenities = listOf("Hot Water Geyser", "Wi-Fi in Lobby", "Television", "Room Heater"),
                details = "Clean, hygienic budget rooms located near Dal Gate and market areas.",
                imageUrls = emptyList()
            ),
            isDemo = true
        ),
        TourPackage(
            id = "pkg_kerala_backwaters",
            agencyId = "ag_malabar",
            destinationId = "dest_kerala",
            title = "Enchanting Kerala: Munnar Hills & Alleppey Private Houseboat",
            days = 5,
            nights = 4,
            pricePerPerson = 21500L,
            inclusions = listOf(
                "Dedicated AC Sedan (Etios/Dzire) from Kochi to Kochi",
                "2 Nights in Munnar Tea Plantation Luxury Resort",
                "1 Night Thekkady Spice Sanctuary Hotel",
                "1 Night Private Air-Conditioned Deluxe Houseboat in Alleppey",
                "All 3 Meals during Houseboat stay (Authentic Kerala Sadhya/Karimeen)",
                "Daily Breakfast at Hotels",
                "Kathakali & Kalaripayattu Martial Arts cultural show tickets"
            ),
            exclusions = listOf(
                "Flight / Train tickets",
                "Boating in Periyar Lake (Thekkady)",
                "Personal expenses & camera permits"
            ),
            itinerary = listOf(
                ItineraryDay(1, "Kochi to Munnar Mist Hills (130 km / 4 hrs)", "Pickup at Cochin Airport/Ernakulam. En route visit Cheeyappara and Valara waterfalls. Check into plantation resort."),
                ItineraryDay(2, "Munnar Tea Estates & Eravikulam National Park", "Spot the endangered Nilgiri Tahr at Rajamalai. Visit Tea Museum, Mattupetty Dam, and Echo Point."),
                ItineraryDay(3, "Munnar to Thekkady Spice Plantations", "Drive to Periyar. Guided spice walk through cardamom, clove, and pepper gardens. Evening martial arts show."),
                ItineraryDay(4, "Thekkady to Alleppey Houseboat Cruise", "Board your private houseboat at 12 PM. Cruise along scenic paddy fields, narrow canals, and coconut lagoons. Freshly cooked onboard meals."),
                ItineraryDay(5, "Alleppey to Cochin Drop", "Disembark at 9:00 AM after lakeside breakfast. Visit Fort Kochi Chinese Fishing Nets before flight.")
            ),
            cancellationPolicy = "100% refund up to 10 days before travel date.",
            maxGroupSize = 4,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=800&q=80"
            ),
            rating = 4.8,
            active = true,
            vehicle = VehicleInfo(
                type = "Sedan",
                vehicleName = "Toyota Etios / Dzire AC",
                ac = true,
                seatingCapacity = 4,
                stationOrAirportPickup = true,
                details = "Dedicated air-conditioned cab for all transfers and sightseeing from Kochi Airport/Railway station.",
                imageUrl = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80"
            ),
            food = FoodInfo(
                mealPlan = "Breakfast + Dinner",
                cuisine = "Veg & Non-veg",
                jainOnRequest = true,
                details = "Traditional Kerala banana leaf meals, Malabar parotta, appam with stew, and fresh coconut curries with choice of chicken/fish or pure veg."
            ),
            hotel = HotelInfo(
                hotelName = "Munnar Tea Mist Resort & Lake Ripples Houseboat",
                category = "4-star",
                roomType = "Valley View Balcony Suite & Private AC Bedroom",
                occupancy = "Double sharing",
                amenities = listOf("Balcony View", "Swimming Pool", "Spa & Ayurveda", "Wi-Fi", "Electric Kettle", "Power Backup"),
                details = "Lush hill resort perched inside tea slopes, followed by a luxury motorized thatch-roof houseboat on Vembanad Lake.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1000&q=80"
                )
            ),
            isDemo = true
        ),
        TourPackage(
            id = "pkg_rajasthan_royal",
            agencyId = "ag_royalmarwar",
            destinationId = "dest_rajasthan",
            title = "Golden Heritage: Jaipur Amber Fort, Jodhpur & Udaipur Lakes",
            days = 7,
            nights = 6,
            pricePerPerson = 34000L,
            inclusions = listOf(
                "Chauffeur-driven AC Sedan / SUV",
                "Heritage Haveli & 4-Star Palace Hotels",
                "Daily Breakfast & Folk Dance Gala Dinner in Jodhpur",
                "Camel ride and desert sunset over dunes",
                "Sunset Boat Cruise on Lake Pichola in Udaipur",
                "Government licensed local guides at Amber Fort & Mehrangarh"
            ),
            exclusions = listOf(
                "Entry monument tickets",
                "Lunches",
                "Personal tips & porterage"
            ),
            itinerary = listOf(
                ItineraryDay(1, "Jaipur Royal Pink City Welcome", "Pickup and transfer to heritage hotel. Evening visit to Birla Temple and Chokhi Dhani cultural village."),
                ItineraryDay(2, "Jaipur Forts & Palaces", "Amber Fort with elephant/jeep transfer, Hawa Mahal photo-stop, City Palace, and Jantar Mantar."),
                ItineraryDay(3, "Jaipur to Jodhpur Blue City via Pushkar", "Visit holy Pushkar Brahma Temple and lake. Arrive in Jodhpur under the shadows of Mehrangarh."),
                ItineraryDay(4, "Mehrangarh Fort & Jaswant Thada", "Explore grand courtyards and royal armoury. Walk through Clock Tower blue lanes."),
                ItineraryDay(5, "Jodhpur to Udaipur City of Lakes", "En route visit magnificent marble Jain temples of Ranakpur. Evening check-in at Lake Pichola hotel."),
                ItineraryDay(6, "Udaipur Romance & Saheliyon-ki-Bari", "City Palace Udaipur, Jagdish Temple, vintage car museum, and evening boat ride on Lake Pichola."),
                ItineraryDay(7, "Udaipur Departure", "Breakfast and airport/station transfer.")
            ),
            cancellationPolicy = "Free cancellation up to 10 days before tour start date.",
            maxGroupSize = 6,
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=800&q=80"
            ),
            rating = 4.9,
            active = true,
            vehicle = VehicleInfo(
                type = "Sedan",
                vehicleName = "Honda City / Maruti Ciaz (Dual AC)",
                ac = true,
                seatingCapacity = 4,
                stationOrAirportPickup = true,
                details = "Executive sedan with pushback comfort seats, complimentary mineral water, and knowledgeable Rajasthani chauffeur.",
                imageUrl = ""
            ),
            food = FoodInfo(
                mealPlan = "Breakfast only",
                cuisine = "Veg",
                jainOnRequest = true,
                details = "Authentic Rajasthani delicacies including Dal Baati Churma, Gatte ki Sabzi, Ker Sangri, and extensive Continental breakfast spread."
            ),
            hotel = HotelInfo(
                hotelName = "Alsisar Haveli Jaipur & Karohi Haveli Udaipur",
                category = "Heritage",
                roomType = "Royal Heritage Suite",
                occupancy = "Double sharing",
                amenities = listOf("Heritage Courtyard", "Outdoor Pool", "Antique Furniture", "Free Wi-Fi", "Lakeside Terrace", "Doctor on Call"),
                details = "Restored 19th-century royal palaces with traditional frescoes, carved balconies, and marble fountains.",
                imageUrls = emptyList()
            ),
            isDemo = true
        )
    )

    fun getSampleDepartures(): List<Departure> {
        val list = mutableListOf<Departure>()
        val cal = Calendar.getInstance()

        // Generate departures for today and next 60 days
        for (i in 1..45 step 3) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, i)
            val ts = Timestamp(c.time)

            list.add(
                Departure(
                    id = "dep_kash_lux_$i",
                    packageId = "pkg_kashmir_luxury",
                    agencyId = "ag_himalayan",
                    destinationId = "dest_kashmir",
                    date = ts,
                    seatsTotal = 16,
                    seatsBooked = (2 + (i % 7) * 2),
                    seatsHeld = (i % 3),
                    priceOverride = if (i % 9 == 0) 26999L else null,
                    status = when {
                        i % 11 == 0 -> "full"
                        i % 5 == 0 -> "limited"
                        else -> "open"
                    },
                    isDemo = true
                )
            )

            list.add(
                Departure(
                    id = "dep_kash_bud_$i",
                    packageId = "pkg_kashmir_budget",
                    agencyId = "ag_budgetbharat",
                    destinationId = "dest_kashmir",
                    date = ts,
                    seatsTotal = 20,
                    seatsBooked = (6 + (i % 5) * 2),
                    seatsHeld = 1,
                    priceOverride = null,
                    status = if (i % 7 == 0) "limited" else "open",
                    isDemo = true
                )
            )

            list.add(
                Departure(
                    id = "dep_ker_back_$i",
                    packageId = "pkg_kerala_backwaters",
                    agencyId = "ag_malabar",
                    destinationId = "dest_kerala",
                    date = ts,
                    seatsTotal = 12,
                    seatsBooked = (1 + (i % 4) * 2),
                    seatsHeld = 0,
                    priceOverride = if (i % 6 == 0) 19999L else null,
                    status = if (i % 13 == 0) "full" else "open",
                    isDemo = true
                )
            )

            list.add(
                Departure(
                    id = "dep_raj_roy_$i",
                    packageId = "pkg_rajasthan_royal",
                    agencyId = "ag_royalmarwar",
                    destinationId = "dest_rajasthan",
                    date = ts,
                    seatsTotal = 14,
                    seatsBooked = (3 + (i % 3) * 3),
                    seatsHeld = 1,
                    priceOverride = null,
                    status = "open",
                    isDemo = true
                )
            )
        }
        return list
    }
}
