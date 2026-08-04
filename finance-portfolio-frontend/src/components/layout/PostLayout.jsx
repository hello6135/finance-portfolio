import React from 'react';
import { Outlet } from 'react-router';
import CategoryNavbar from './CategoryNavbar';

const PostLayout = () => {
    return (
        <>
            <CategoryNavbar />
            <div className="flex-grow-1 d-flex flex-column">
                <Outlet />
            </div>
        </>
    );
};

export default PostLayout;