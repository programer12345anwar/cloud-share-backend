☁️ Cloud-Share Application

A secure cloud-based file sharing platform that enables users to upload, store, manage, and share files seamlessly.
Built using Spring Boot + React + Microservices architecture, this application integrates Clerk Authentication, JWT security, Razorpay payments, and Swagger API documentation.

📂 Repositories
🔹 Backend

🔗 https://github.com/programer12345anwar/cloud-share-backend

🔹 Frontend

🔗 https://github.com/programer12345anwar/cloud-share-web-app



🚀 Key Features

✅ Authentication & Security

Clerk authentication (Frontend)

JWT-based authentication (Backend)

Secure API access

Token validation & request filtering

Role-based access 



✅ File Management

Upload files securely

Download shared files

Manage user files

Cloud-based file storage

Metadata handling



✅ Payment Integration

Razorpay payment gateway

Subscription / premium user support

Payment verification APIs

Secure transaction handling



✅ API Management

Swagger & OpenAPI documentation

Versioned REST APIs

Clean controller architecture



✅ Architecture

Modular backend structure

Stateless APIs

Separation of concerns

Scalable design approach



🛠️ Tech Stack
Backend

Java

Spring Boot

JWT Authentication

Razorpay API

Swagger (OpenAPI)

MongoDB

Maven

REST Architecture

Frontend

React.js

Clerk Authentication

Tailwind / CSS

Axios

Razorpay Checkout UI

Responsive design


🔧 Backend Setup
✅ Clone Repository
git clone https://github.com/programer12345anwar/cloud-share-backend.git
cd cloud-share-backend

✅ Create .env file

Create a .env file in backend root directory:

RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=

✅ Run Backend
mvn spring-boot:run

✅ Swagger Documentation

Once server is running, open:

http://localhost:8080/swagger-ui.html



🎨 Frontend Setup
✅ Clone Repository
git clone https://github.com/programer12345anwar/cloud-share-web-app.git
cd cloud-share-web-app

✅ Install Dependencies
npm install

✅ Start Frontend Server
npm start

🌐 Application Flow
User → Clerk Auth (Frontend)
     → JWT Token Issued
     → Backend APIs
     → File Services + DB
     → Razorpay Payments
     → Secure File Access

🔐 Security Practices

Authentication handled by Clerk

Authorization via JWT

Environment-based secret management

Token filtering middleware

Payment validation on server-side

Swagger protected endpoints

📁 Project Structure
Backend
cloud-share-backend
 ┣ controller
 ┣ services
 ┣ repositories
 ┣ models
 ┣ config
 ┣ security
 ┗ utils

Frontend
cloud-share-web-app
 ┣ src
 ┣ pages
 ┣ components
 ┣ services
 ┗ App.jsx

📌 Future Enhancements

Admin dashboard

File search and filtering

Cloud provider integration

AI-based content tagging

Email notifications

Shareable file links

File expiry system

🧑‍💻 Author

MD Anwar Alam
Java Backend Developer
Spring Boot | Microservices | JWT | REST | Razorpay | Clerk | PostgreSQL
📍 India

📜 License

This project is open-source for educational and personal use.

⭐ Support

If you like this project:

⭐ Star the repo
📢 Share it
🚀 Follow for more projects
