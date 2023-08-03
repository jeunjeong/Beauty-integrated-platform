package com.palette.palette.domain.item.service;

import com.palette.palette.domain.category.entity.Category;
import com.palette.palette.domain.category.repository.CategoryRepository;
import com.palette.palette.domain.item.dto.ItemAddReqDto;
import com.palette.palette.domain.item.dto.ItemGetResDto;
import com.palette.palette.domain.item.dto.ItemOptionAddReqDto;
import com.palette.palette.domain.item.entity.Item;
import com.palette.palette.domain.item.repository.ItemRepository;
import com.palette.palette.domain.itemDetailPhoto.entity.ItemDetailPhoto;
import com.palette.palette.domain.itemDetailPhoto.repository.ItemDetailPhotoRepository;
import com.palette.palette.domain.itemOption.entity.ItemOption;
import com.palette.palette.domain.itemOption.repository.ItemOptionRepository;
import com.palette.palette.domain.itemPhoto.entity.ItemPhoto;
import com.palette.palette.domain.itemPhoto.repository.ItemPhotoRepository;
import com.palette.palette.domain.user.entity.User;
import com.palette.palette.domain.user.repository.UserRepository;
import com.palette.palette.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ItemRepository itemRepository;
    private final ItemDetailPhotoRepository itemDetailPhotoRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemPhotoRepository itemPhotoRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Boolean registItem(HttpServletRequest request, ItemAddReqDto itemAddReqDto){
        String userEmail = jwtTokenProvider.getUserEmail(jwtTokenProvider.resolveToken(request));
        Optional<User> user = userRepository.findByEmail(userEmail);
        Item item = Item.toEntity(itemAddReqDto, user.get());
        itemRepository.save(item);

        Optional<Category> category = categoryRepository.findById(itemAddReqDto.getCategoryParentId());
        categoryRepository.save(Category.toEntity(itemAddReqDto.getCategoryName(), category.get()));


        List<ItemOption> itemOptionList = new ArrayList<>();
        for(ItemOptionAddReqDto iard : itemAddReqDto.getItemOptionList()){
            ItemOption itemOption = ItemOption.toEntity(iard, item);
            itemOptionList.add(itemOption);
        }
        itemOptionRepository.saveAll(itemOptionList);

        List<ItemDetailPhoto> itemDetailPhotoList = new ArrayList<>();
        for(String s : itemAddReqDto.getItemDetailImageList()){
            ItemDetailPhoto itemDetailPhoto = ItemDetailPhoto.toEntity(s, item);
            itemDetailPhotoList.add(itemDetailPhoto);
        }
        itemDetailPhotoRepository.saveAll(itemDetailPhotoList);

        List<ItemPhoto> itemPhotoList = new ArrayList<>();
        for(String s : itemAddReqDto.getItemPhotoList()){
            ItemPhoto itemPhoto = ItemPhoto.toEntity(s, item);
            itemPhotoList.add(itemPhoto);
        }
        itemPhotoRepository.saveAll(itemPhotoList);

        return true;
    }

    public List<ItemGetResDto> getItem(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        List<ItemGetResDto> items = itemRepository.findAllOrderByEndAtAsc(pageable)
                .getContent()
                .stream()
                .map(ItemGetResDto::toDto)
                .collect(Collectors.toList());
        return items;
    }
}
