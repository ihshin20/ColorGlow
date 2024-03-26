from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
import time
import urllib.request
import os

s = Service("C:\Crawling\chromedriver.exe") # chromdriver dir
driver = webdriver.Chrome(service=s)

output_folder_path = 'C:\Crawling\preprocessing\Dark_Autumn_add' # 다운로드 할 폴더
search_query = '배우 한고은' # 검색어
img_num = 1000 # 개수

def fetch_image_urls_google(driver, max_images, query):

    driver.get("https://www.google.com/imghp?hl=en")
    search_box = driver.find_element(By.NAME, "q")
    search_box.send_keys(query)
    search_box.send_keys(Keys.ENTER)

    # 검색 결과 로딩 대기(네트워크 상태에 따라)
    #time.sleep(2)  

    image_urls = set()
    last_height = driver.execute_script("return document.body.scrollHeight")

    while len(image_urls) < max_images:
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")

        # 로딩 대기 (네트워크 상태에 따라)
        #time.sleep(4) 
        
        WebDriverWait(driver, 5).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "img.Q4LuWd"))
        )

        # 지정한 개수 만큼의 이미지가 있으면 스크롤 중지
        thumbnail_results = driver.find_elements(By.CSS_SELECTOR, "img.Q4LuWd")
        if len(thumbnail_results) > max_images:
            print("Find all images of a specified number.")
            break
        
        new_height = driver.execute_script("return document.body.scrollHeight")
        if new_height == last_height:
            print("Reached the end of the page.")
            break
        last_height = new_height

        # "Show more results" 버튼이 나타날 때까지 기다린 후 클릭
        try:
            # "Show more results" 버튼 클릭
            more_results_button = WebDriverWait(driver, 10).until(
                EC.element_to_be_clickable((By.XPATH, "//input[@value='Show more results']"))
            )
            if more_results_button:
                driver.execute_script("arguments[0].click();", more_results_button)
                WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "새 이미지 로드 확인을 위한 셀렉터"))
                )
                driver.execute_script("arguments[0].click();", more_results_button)
        except TimeoutException:
            # "Show more results" 버튼이 타임아웃으로 인해 발견되지 않는 경우, 스크롤로 대체
            print("No more results button found by timeout.")
            pass


    thumbnail_results = driver.find_elements(By.CSS_SELECTOR, "img.Q4LuWd")
    for img in thumbnail_results[len(image_urls):]:
        try:
            # 썸네일 클릭
            img.click()
            # 상세 이미지 URL 추출
            actual_image = WebDriverWait(driver, 5).until(
                EC.visibility_of_element_located((By.CSS_SELECTOR, "img.sFlh5c.pT0Scc.iPVvYb"))
            )
            if actual_image.get_attribute('src') and 'http' in actual_image.get_attribute('src'):
                image_url = actual_image.get_attribute('src')
                image_urls.add(image_url)
                if len(image_urls) >= max_images:
                    break
        except Exception as e:
            print(f"Error fetching detail image URL: {e}")


    return list(image_urls)[:max_images]



def fetch_image_urls_naver(driver, max_images, query):

    driver.get("https://search.naver.com/search.naver?where=image&sm=tab_jum&query=")
    search_box = driver.find_element(By.NAME, "query")
    search_box.send_keys(query)
    search_box.send_keys(Keys.ENTER)

    # 검색 결과 로딩 대기(네트워크 상황에 따라)
    #time.sleep(2)  

    image_urls = set()
    last_height = driver.execute_script("return document.body.scrollHeight")

    while len(image_urls) < max_images:
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        # 로딩 대기(네트워크 상황에 따라)
        #time.sleep(4) 

        WebDriverWait(driver, 12).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "img._fe_image_tab_content_thumbnail_image"))
        )

        thumbnail_results = driver.find_elements(By.CSS_SELECTOR, "img._fe_image_tab_content_thumbnail_image")
        if len(thumbnail_results) > max_images:
            print("Find all images of a specified number.")
            break

        new_height = driver.execute_script("return document.body.scrollHeight")
        if new_height == last_height:
            print("Reached the end of the page.")
            break
        last_height = new_height

    thumbnail_results = driver.find_elements(By.CSS_SELECTOR, "img._fe_image_tab_content_thumbnail_image")
    for img in thumbnail_results[len(image_urls):]:
        try:
            img.click()

            actual_image = WebDriverWait(driver, 10).until(
                EC.visibility_of_element_located((By.CSS_SELECTOR, "img._fe_image_viewer_image_fallback_target"))
            )
            if actual_image.get_attribute('src') and 'http' in actual_image.get_attribute('src'):
                image_url = actual_image.get_attribute('src')
                image_urls.add(image_url)
                if len(image_urls) >= max_images:
                    break
        except Exception as e:
            print(f"Error fetching detail image URL: {e}")

    return list(image_urls)[:max_images]

def download_images(image_urls, save_path, engine, search_query):
    if not os.path.exists(save_path):
        os.makedirs(save_path)

    for i, url in enumerate(image_urls):
        filename = f"{search_query}_{engine}_{i}.jpg" # 저장 될 파일 이름
        try:
            urllib.request.urlretrieve(url, os.path.join(save_path, filename))
            print(f"Downloaded image {filename}")
        except Exception as e:
            print(f"Error downloading image {filename}: {e}")

try:
    
    
    urls_google = fetch_image_urls_google(driver, img_num, search_query)   
    urls_naver = fetch_image_urls_naver(driver, img_num, search_query)   

    download_images(urls_google, output_folder_path, "google", search_query)
    download_images(urls_naver, output_folder_path, "naver", search_query)
finally:
    driver.quit()