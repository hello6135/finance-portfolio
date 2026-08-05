import React from 'react';
import { Outlet } from 'react-router';
import AdminNavbar from './AdminNavbar';

const AdminLayout = () => {
    return (
        <>
            <AdminNavbar />
            <div className="flex-grow-1 d-flex flex-column">
                <Outlet />
            </div>
        </>
    );
};

export default AdminLayout;