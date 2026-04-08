import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

const Layout = () => {
    return (
        <div className="d-flex flex-column bg-theme-custom"
            style={{ minHeight: 'calc(100vh - 56px)' }}>
            <Navbar />
            <main className="flex-grow-1 d-flex flex-column">
                <Outlet />
            </main>
        </div>
    );
};

export default Layout;