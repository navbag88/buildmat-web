import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { getToken } from './utils/auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Customers from './pages/Customers'
import Products from './pages/Products'
import Invoices from './pages/Invoices'
import InvoiceForm from './pages/InvoiceForm'
import Payments from './pages/Payments'
import Reports from './pages/Reports'
import Users from './pages/Users'

const PrivateRoute = ({ children }) =>
  getToken() ? children : <Navigate to="/login" replace />

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard"  element={<Dashboard />} />
        <Route path="customers"  element={<Customers />} />
        <Route path="products"   element={<Products />} />
        <Route path="invoices"   element={<Invoices />} />
        <Route path="invoices/new"       element={<InvoiceForm />} />
        <Route path="invoices/:id/edit"  element={<InvoiceForm />} />
        <Route path="payments"   element={<Payments />} />
        <Route path="reports"    element={<Reports />} />
        <Route path="users"      element={<Users />} />
      </Route>
    </Routes>
  )
}
