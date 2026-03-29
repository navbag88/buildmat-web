# 🏗 BuildMat Billing — Web Application

Full-stack web app converted from the JavaFX desktop billing software.  
**Stack:** Spring Boot 3 + React 18 + Tailwind CSS + MySQL (Railway)

---

## 🚀 Deploy to Railway (5 minutes)

### Option A — GitHub + Railway (Recommended)

1. **Push this project to GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/buildmat-web.git
   git push -u origin main
   ```

2. **Create Railway project**
   - Go to https://railway.app → New Project → Deploy from GitHub
   - Select your repo

3. **Add MySQL database**
   - In Railway dashboard → New → Database → MySQL
   - Railway auto-injects `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`

4. **Set environment variables** in Railway → Variables:
   ```
   SPRING_PROFILES_ACTIVE=railway
   DATABASE_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&allowPublicKeyRetrieval=true
   DATABASE_USERNAME=${MYSQLUSER}
   DATABASE_PASSWORD=${MYSQLPASSWORD}
   JWT_SECRET=xW+PEQ8gmKGEYdZ8DOqBjipTDRxKh3/B9oodNsLiXN7Kh8oBDnoHZVBiF+J7E8zn
   FRONTEND_URL=https://your-app.railway.app
   ```

5. **Deploy** — Railway auto-builds and deploys. Done!

---

### Option B — Docker on Railway

Railway auto-detects the `Dockerfile` if present.

1. Push to GitHub (same as above)
2. In Railway → New → Deploy from GitHub → select repo
3. Railway uses the Dockerfile automatically
4. Add MySQL service + same env vars as above

---

## 🛠 Local Development

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+

### Run backend (uses H2 in-memory DB locally)
```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
# H2 console: http://localhost:8080/h2-console
```

### Run frontend
```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
# Auto-proxies /api calls to backend
```

### Build production (frontend bundled into backend JAR)
```bash
cd frontend && npm run build        # builds to backend/src/main/resources/static/
cd backend  && mvn clean package -DskipTests
java -jar target/buildmat-web-1.0.0.jar
# App at http://localhost:8080
```

---

## 📋 Features

| Module | Features |
|---|---|
| **Login** | JWT auth, role-based (ADMIN/USER) |
| **Dashboard** | Revenue stats, invoice chart, recent invoices |
| **Invoices** | Create/edit with line items, SGST/CGST, PDF download |
| **Customers** | CRUD, Excel import/export, PDF export |
| **Products** | CRUD with GST config, stock tracking, import/export |
| **Payments** | Record payments, method tracking, export |
| **MIS Reports** | 6 reports: Sales, Outstanding, Customer, Product, GST, Payments — Excel + PDF |
| **Users** | Admin-only user management, roles, password change |

---

## 🔐 Default Credentials

```
Username: admin
Password: admin123
```
Change immediately after first login via Users → Change Password.

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/login | Login |
| GET | /api/dashboard/stats | Dashboard stats |
| GET/POST | /api/customers | List / Create |
| GET/PUT/DELETE | /api/customers/{id} | Get / Update / Delete |
| POST | /api/customers/import | Import from Excel |
| GET | /api/customers/export/excel | Export to Excel |
| GET | /api/customers/export/pdf | Export to PDF |
| *(same pattern)* | /api/products/* | Product CRUD + import/export |
| *(same pattern)* | /api/invoices/* | Invoice CRUD + PDF |
| *(same pattern)* | /api/payments/* | Payment CRUD |
| GET | /api/reports/sales-summary | Sales report |
| GET | /api/reports/outstanding | Outstanding invoices |
| GET | /api/reports/customer-sales | Customer sales |
| GET | /api/reports/product-sales | Product sales |
| GET | /api/reports/gst | GST report |
| GET | /api/reports/payment-collection | Payment collection |
| GET | /api/reports/{type}/export/excel | Export any report |
| GET | /api/reports/{type}/export/pdf | Export any report |

---

## 🗄 Environment Variables

| Variable | Description | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Use `railway` for MySQL | `railway` |
| `DATABASE_URL` | JDBC URL | `jdbc:mysql://host:port/db` |
| `DATABASE_USERNAME` | DB username | `root` |
| `DATABASE_PASSWORD` | DB password | `secret` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `MySecretKey...` |
| `PORT` | Server port (auto-set by Railway) | `8080` |
| `FRONTEND_URL` | CORS allowed origin | `https://app.railway.app` |
