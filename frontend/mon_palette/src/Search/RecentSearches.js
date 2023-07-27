import React from 'react';
import './RecentSearches.css'

const RecentSearches = ({ searches, onRemove, onSearchFromRecent }) => {
  const handleSearch = (search) => {
    onSearchFromRecent(search);
  };

  return (
    <div className="recent-searches">
      <h3>최근 검색어</h3>
      <ul>
        {searches.map((search, index) => (
          <li key={index} onClick={() => handleSearch(search)}>
            {search}
            <button onClick={() => onRemove(index)}>X</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default RecentSearches;

